package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.Instant;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.domain.response.payment.ResPaymentVNPAYDTO;
import com.tuna.ecommerce.domain.Payment;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.order.ResGetOrderDTO;
import com.tuna.ecommerce.repository.CartItemRepository;
import com.tuna.ecommerce.repository.CouponRepository;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import com.tuna.ecommerce.domain.ProductVariant;
import org.springframework.context.annotation.Lazy;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserService userService;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final AddressService addressService;
    private final GHTKService ghtkService;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public OrderService(
            OrderRepository orderRepository,
            CartService cartService,
            UserService userService,
            CartItemRepository cartItemRepository,
            CouponRepository couponRepository,
            AddressService addressService,
            GHTKService ghtkService,
            NotificationService notificationService,
            @Lazy InventoryService inventoryService,
            @Lazy PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.userService = userService;
        this.cartItemRepository = cartItemRepository;
        this.couponRepository = couponRepository;
        this.addressService = addressService;
        this.ghtkService = ghtkService;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    public ResultPaginationDTO fetchOrdersByUser(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        User user = this.userService.findByUsername(email);

        Page<Order> pageOrder = this.orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageOrder.getTotalPages());
        mt.setTotal(pageOrder.getTotalElements());

        rs.setMeta(mt);

        List<ResGetOrderDTO> listOrder = pageOrder.getContent()
                .stream().map(item -> this.convertToResGetOderDTO(item))
                .collect(java.util.stream.Collectors.toList());

        rs.setResult(listOrder);

        return rs;
    }

    public ResultPaginationDTO fetchAllOrders(Pageable pageable, OrderStatusEnum status, Instant startDate,
            Instant endDate) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> pageOrder = this.orderRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageOrder.getTotalPages());
        mt.setTotal(pageOrder.getTotalElements());

        rs.setMeta(mt);

        List<ResGetOrderDTO> listOrder = pageOrder.getContent()
                .stream().map(item -> this.convertToResGetOderDTO(item))
                .collect(java.util.stream.Collectors.toList());

        rs.setResult(listOrder);
        return rs;
    }

    public ResGetOrderDTO createOder(ReqCheckoutDTO req, HttpServletRequest request) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        User user = this.userService.findByUsername(email);
        Address address = this.addressService.getAddressById(req.getAddressId());
        if (address == null) {
            throw new IdInvalidException("Địa chỉ không tồn tại.");
        }

        List<CartItem> cartItems = cartItemRepository.findByIdIn(req.getCartItemId());
        if (cartItems.isEmpty()) {
            throw new IdInvalidException("Giỏ hàng của bạn đang trống.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setReceiverName(address.getReceiverName());
        order.setPhone(address.getPhone());
        order.setProvince(address.getProvince());
        order.setDistrict(address.getDistrict());
        order.setWard(address.getWard());
        order.setShippingAddress(address.getDetail());
        order.setStatus(OrderStatusEnum.PENDING);
        order.setPaymentStatus(PaymentStatusEnum.UNPAID);
        order.setDiscountPrice(BigDecimal.ZERO);

        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem i : cartItems) {
            Product product = i.getProduct();
            ProductVariant variant = i.getProductVariant();
            
            // Validate stock from inventory directly
            int currentStock = this.inventoryService.getCurrentStock(
                product.getId(), 
                (variant != null) ? variant.getId() : null
            );
            if (currentStock < i.getQuantity()) {
                throw new IdInvalidException("Sản phẩm " + product.getName() + " không đủ hàng. Còn lại: " + currentStock);
            }

            // Reserve from Inventory (Available -> Reserved)
            this.inventoryService.reserveStock(
                product.getId(), 
                (variant != null) ? variant.getId() : null, 
                i.getQuantity()
            );

            product.setSoldCount(product.getSoldCount() + i.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(i.getQuantity());
            orderItem.setPrice(i.getUnitPrice());
            orderItem.setSubTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            subTotal = subTotal.add(orderItem.getSubTotal());

            order.addOrderItem(orderItem);
        }

        order.setTotalPrice(subTotal);

        // Shipping logic
        double weight = this.cartService.calculateTotalWeight(cartItems);
        int shippingFeeRaw = this.ghtkService.calculateFee(address.getProvince(), address.getDistrict(), (int) weight);
        BigDecimal shippingFee = (subTotal.compareTo(BigDecimal.valueOf(500000)) > 0) ? BigDecimal.ZERO : BigDecimal.valueOf(shippingFeeRaw);
        order.setShippingFee(shippingFee.intValue());

        // Coupon logic
        String couponCode = req.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = this.couponRepository.findByCode(couponCode)
                    .orElseThrow(() -> new IdInvalidException("Mã giảm giá không tồn tại: " + couponCode));

            if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new IdInvalidException("Mã giảm giá đã hết lượt sử dụng");
            }
            if (coupon.getMinOrderValue() != null && subTotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new IdInvalidException("Đơn hàng chưa đủ giá trị tối thiểu để áp dụng mã.");
            }
            if (coupon.getStatus() != com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE) {
                throw new IdInvalidException("Mã giảm giá không còn hoạt động");
            }
            if (coupon.getEndDate().isBefore(LocalDateTime.now())) {
                throw new IdInvalidException("Mã giảm giá đã hết hạn");
            }

            BigDecimal discountAmount = calculateDiscount(coupon, subTotal);
            order.setDiscountPrice(discountAmount);
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            this.couponRepository.save(coupon);
        }

        BigDecimal finalPrice = subTotal.add(shippingFee).subtract(order.getDiscountPrice());
        order.setFinalPrice(finalPrice);
        Order savedOrder = this.orderRepository.save(order);

        // Payment Handling (Synchronous with Order Creation)
        ResGetOrderDTO resDTO = this.convertToResGetOderDTO(savedOrder);
        Payment payment;
        switch (req.getPaymentMethod()) {
            case VNPAY:
                payment = this.paymentService.createPendingVNPayPayment(savedOrder.getId());
                ResPaymentVNPAYDTO vnpayRes = this.paymentService.createVnPayPayment(request, payment.getId());
                if (!"00".equals(vnpayRes.getCode())) {
                    throw new IdInvalidException("Lỗi khởi tạo thanh toán VNPay: " + vnpayRes.getMessage());
                }
                resDTO.setPaymentUrl(vnpayRes.getPaymentUrl());
                resDTO.setPaymentMethod("VNPAY");
                break;
            case COD:
            default:
                this.paymentService.createCODPayment(savedOrder.getId());
                resDTO.setPaymentMethod("COD");
                break;
        }
        // Finalize order to save payment association
        this.orderRepository.save(savedOrder);

        // Only clear cart if everything is successful
        this.cartItemRepository.deleteAll(cartItems);

        // Gửi thông báo cho User
        this.notificationService.createNotification(
            user, 
            "Đặt hàng thành công", 
            "Đơn hàng #" + savedOrder.getId() + " của bạn đã được tiếp nhận và đang chờ xử lý.", 
            "ORDER_SUCCESS"
        );

        return resDTO;
    }

    public Order getOrder(Long id) {
        Optional<Order> orderOptional = this.orderRepository.findById(id);
        return orderOptional.isPresent() ? orderOptional.get() : null;
    }

    public ResGetOrderDTO convertToResGetOderDTO(Order order) {
        ResGetOrderDTO res = new ResGetOrderDTO();
        res.setId(order.getId());

        if (order.getUser() != null) {
            ResGetOrderDTO.UserInner userInner = new ResGetOrderDTO.UserInner();
            userInner.setId(order.getUser().getId());
            if (order.getUser().getUserProfile() != null) {
                userInner.setName(order.getUser().getUserProfile().getName());
            }
            userInner.setEmail(order.getUser().getEmail());
            res.setUser(userInner);
        }

        res.setStatus(order.getStatus());
        res.setSubTotal(order.getTotalPrice());
        res.setShippingFee(order.getShippingFee());
        res.setTotalPrice(order.getFinalPrice());
        res.setReceiverName(order.getReceiverName());
        res.setPhone(order.getPhone());
        res.setPaymentStatus(order.getPaymentStatus());
        res.setShippingAddress(order.getShippingAddress());

        if (order.getPayment() != null) {
            res.setTransactionID(order.getPayment().getTransactionId());
            if (order.getPayment().getMethod() != null) {
                res.setPaymentMethod(order.getPayment().getMethod().name());
            }
        }
        res.setCreatedAt(order.getCreatedAt());

        if (order.getItems() != null) {
            List<ResGetOrderDTO.OrderItemInner> itemDTOs = order.getItems().stream().map(item -> {
                ResGetOrderDTO.OrderItemInner itemDTO = new ResGetOrderDTO.OrderItemInner();
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());

                // Get first image if available
                if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                    itemDTO.setProductImage(item.getProduct().getImages().get(0).getImageUrl());
                }

                return itemDTO;
            }).collect(java.util.stream.Collectors.toList());
            res.setItems(itemDTOs);
        }

        return res;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal total) {
        BigDecimal discount;
        if (coupon.getType() == CouponTypeEnum.PERCENT) {
            discount = coupon.getDiscountValue().multiply(total).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountValue() != null && discount.compareTo(coupon.getMaxDiscountValue()) > 0) {
                discount = coupon.getMaxDiscountValue();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        return discount;
    }

    public Order handleUpdateStatus(Long id, OrderStatusEnum status) throws IdInvalidException {
        Order order = this.getOrder(id);
        if (order == null) {
            throw new IdInvalidException("Order not found with id: " + id);
        }

        order.setStatus(status);
        order = this.orderRepository.save(order);

        // Gửi thông báo tự động dựa trên trạng thái mới
        String title = "";
        String message = "";
        String type = "ORDER_STATUS_UPDATE";

        switch (status) {
            case CONFIRMED:
                title = "Đơn hàng đã được xác nhận";
                message = "Đơn hàng #" + order.getId() + " của bạn đã được xác nhận.";
                // Commit stock for each item
                for (OrderItem item : order.getItems()) {
                    this.inventoryService.commitStock(
                            item.getProduct().getId(),
                            item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                            item.getQuantity(),
                            "Xác nhận đơn hàng #" + order.getId());
                }
                break;
            case DELIVERING:
                title = "Đơn hàng đang được giao";
                message = "Đơn hàng #" + order.getId() + " đang trên đường tới bạn.";
                break;
            case DELIVERED:
                title = "Giao hàng thành công";
                message = "Đơn hàng #" + order.getId() + " đã được giao thành công. Cảm ơn bạn!";
                break;
            case CANCELLED:
                title = "Đơn hàng đã bị hủy";
                message = "Rất tiếc, đơn hàng #" + order.getId() + " của bạn đã bị hủy.";
                // Release stock back to available
                for (OrderItem item : order.getItems()) {
                    this.inventoryService.releaseStock(
                            item.getProduct().getId(),
                            item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                            item.getQuantity(),
                            "Hủy đơn hàng #" + order.getId());
                }
                break;
            default:
                break;
        }

        if (!title.isEmpty()) {
            this.notificationService.createNotification(order.getUser(), title, message, type);
        }

        return order;
    }

    public void handleBulkUpdateStatus(List<Long> ids, OrderStatusEnum status) throws IdInvalidException {
        for (Long id : ids) {
            this.handleUpdateStatus(id, status);
        }
    }
}
