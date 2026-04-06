package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.tuna.ecommerce.repository.OrderItemRepository;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.tuna.ecommerce.service.NotificationService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final AddressService addressService;
    private final GHTKService ghtkService;
    private final NotificationService notificationService;

    public ResultPaginationDTO fetchOrdersByUser(Pageable pageable) {
        String email = this.securityUtil.getCurrentUserLogin().orElse(null);
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

    public ResultPaginationDTO fetchAllOrders(Pageable pageable, OrderStatusEnum status) {
        Page<Order> pageOrder;
        if (status != null) {
            pageOrder = this.orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            pageOrder = this.orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

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

    public Order createOder(ReqCheckoutDTO req) throws IdInvalidException {
        String email = this.securityUtil.getCurrentUserLogin().orElse(null);
        User user = this.userService.findByUsername(email);
        Address address = this.addressService.getAddressById(req.getAddressId());
        if (address == null) {
            throw new IdInvalidException("Address not found with id: " + req.getAddressId());
        }

        List<CartItem> cartItems = cartItemRepository.findByIdIn(req.getCartItemId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
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
            if (product.getStock() < i.getQuantity()) {
                throw new RuntimeException("Product " + product.getName() + " is out of stock. Available: " + product.getStock());
            }

            // Decrement stock and increment sold count
            product.setStock(product.getStock() - i.getQuantity());
            product.setSoldCount(product.getSoldCount() + i.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductVariant(i.getProductVariant());
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
                    .orElseThrow(() -> new IdInvalidException("Coupon code not found: " + couponCode));

            if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new IdInvalidException("Coupon usage limit reached");
            }
            if (coupon.getMinOrderValue() != null && subTotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new IdInvalidException("Order value does not meet coupon requirement");
            }
            if (coupon.getStatus() != com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE) {
                throw new IdInvalidException("Coupon is not active");
            }
            if (coupon.getEndDate().isBefore(LocalDateTime.now())) {
                throw new IdInvalidException("Coupon is expired");
            }

            BigDecimal discountAmount = calculateDiscount(coupon, subTotal);
            order.setDiscountPrice(discountAmount);
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            this.couponRepository.save(coupon);
        }

        // Final Price = subTotal + shippingFee - discountPrice
        BigDecimal finalPrice = subTotal.add(shippingFee).subtract(order.getDiscountPrice());
        order.setFinalPrice(finalPrice);

        this.cartItemRepository.deleteAll(cartItems);
        Order savedOrder = this.orderRepository.save(order);

        // Gửi thông báo cho User
        this.notificationService.createNotification(
            user, 
            "Đặt hàng thành công", 
            "Đơn hàng #" + savedOrder.getId() + " của bạn đã được tiếp nhận và đang chờ xử lý.", 
            "ORDER_SUCCESS"
        );

        return savedOrder;
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
                break;
            default:
                break;
        }

        if (!title.isEmpty()) {
            this.notificationService.createNotification(order.getUser(), title, message, type);
        }

        return order;
    }
}
