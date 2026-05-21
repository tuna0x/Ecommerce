package com.tuna.ecommerce.service;

import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.Instant;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.domain.response.payment.ResPaymentVNPAYDTO;
import com.tuna.ecommerce.domain.Payment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.request.order.ReqUpdateOrderAddressDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.order.ResGetOrderDTO;
import com.tuna.ecommerce.repository.CartItemRepository;
import com.tuna.ecommerce.repository.CouponRepository;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.PaymentRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.constant.CouponTypeEnum;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.constant.PaymentStatusEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import com.tuna.ecommerce.domain.ProductVariant;
import org.springframework.context.annotation.Lazy;
import com.tuna.ecommerce.domain.UserCoupon;
import com.tuna.ecommerce.repository.UserCouponRepository;

import org.springframework.data.domain.PageRequest;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@Service
@Transactional
@Slf4j
public class OrderService {
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private OrderService self;

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserService userService;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;
    private final AddressService addressService;
    private final ShippingService shippingService;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserCouponRepository userCouponRepository;
    private final EmailService emailService;
    private final CouponService couponService;
    private final TelegramService telegramService;
    private final FlashSaleService flashSaleService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PayOSService payOSService;

    public OrderService(
            OrderRepository orderRepository,
            CartService cartService,
            UserService userService,
            CartItemRepository cartItemRepository,
            CouponRepository couponRepository,
            AddressService addressService,
            ShippingService shippingService,
            NotificationService notificationService,
            @Lazy InventoryService inventoryService,
            @Lazy PaymentService paymentService,
            PaymentRepository paymentRepository,
            UserCouponRepository userCouponRepository,
            EmailService emailService,
            CouponService couponService,
            TelegramService telegramService,
            FlashSaleService flashSaleService,
            SimpMessagingTemplate messagingTemplate,
            PayOSService payOSService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.userService = userService;
        this.cartItemRepository = cartItemRepository;
        this.couponRepository = couponRepository;
        this.addressService = addressService;
        this.shippingService = shippingService;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.userCouponRepository = userCouponRepository;
        this.emailService = emailService;
        this.couponService = couponService;
        this.telegramService = telegramService;
        this.flashSaleService = flashSaleService;
        this.messagingTemplate = messagingTemplate;
        this.payOSService = payOSService;
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
                .stream().map(item -> this.convertToResGetOrderDTO(item))
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
                // End date is usually 00:00:00. Adjust to the end of the day (23:59:59)
                Instant adjustedEndDate = endDate.truncatedTo(ChronoUnit.DAYS)
                        .plus(23, ChronoUnit.HOURS)
                        .plus(59, ChronoUnit.MINUTES)
                        .plus(59, ChronoUnit.SECONDS);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), adjustedEndDate));
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
                .stream().map(item -> this.convertToResGetOrderDTO(item))
                .collect(java.util.stream.Collectors.toList());

        rs.setResult(listOrder);
        return rs;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order createOrderTransaction(ReqCheckoutDTO req, String email) throws IdInvalidException {
        if (req == null) {
            throw new IdInvalidException("Du lieu checkout khong hop le.");
        }
        if (req.getAddressId() == null) {
            throw new IdInvalidException("Dia chi giao hang khong duoc de trong.");
        }
        if (req.getCartItemId() == null || req.getCartItemId().isEmpty()) {
            throw new IdInvalidException("Gio hang cua ban dang trong.");
        }

        User user = this.userService.findByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Nguoi dung khong hop le.");
        }
        Address address = this.addressService.getAddressById(req.getAddressId());
        if (address == null) {
            throw new IdInvalidException("Địa chỉ không tồn tại.");
        }

        PaymentMethodEnum paymentMethod = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethodEnum.COD;
        List<CartItem> cartItems = cartItemRepository.findByIdIn(req.getCartItemId());
        long requestedItemCount = req.getCartItemId().stream().distinct().count();
        if (cartItems.isEmpty() || cartItems.size() != requestedItemCount) {
            throw new IdInvalidException("Giỏ hàng của bạn đang trống.");
        }

        // TỐI ƯU CỐT LÕI: Sắp xếp cartItems theo ID của ProductVariant/Product thực tế bị khóa trong DB (chứ không phải CartItem ID) để triệt tiêu hoàn toàn nguy cơ Deadlock khi nhiều khách hàng cùng checkout các sản phẩm trùng nhau.
        boolean hasInvalidOwner = cartItems.stream()
                .anyMatch(item -> item.getCart() == null
                        || item.getCart().getUser() == null
                        || !item.getCart().getUser().getId().equals(user.getId()));
        if (hasInvalidOwner) {
            throw new IdInvalidException("Gio hang khong hop le.");
        }

        cartItems.sort((a, b) -> {
            Long idA = (a.getProductVariant() != null) ? a.getProductVariant().getId() : a.getProduct().getId();
            Long idB = (b.getProductVariant() != null) ? b.getProductVariant().getId() : b.getProduct().getId();
            return Long.compare(idA, idB);
        });

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
        order.setConfirmationToken(java.util.UUID.randomUUID().toString());

        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem i : cartItems) {
            Product product = i.getProduct();
            ProductVariant variant = i.getProductVariant();

            // TỐI ƯU CỐT LÕI: Gọi trực tiếp reserveStock để vừa kiểm tra và vừa giữ hàng (reserve) trong 1 lần gọi DB duy nhất, thay vì gọi getCurrentStock kiểm tra trước rồi mới gọi reserveStock (gây thừa 1 vòng truy vấn DB không đáng có).
            try {
                this.inventoryService.reserveStock(
                        product.getId(),
                        (variant != null) ? variant.getId() : null,
                        i.getQuantity());
            } catch (IdInvalidException e) {
                throw new IdInvalidException("Sản phẩm " + product.getName() + " không đủ hàng. " + e.getMessage());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(i.getQuantity());
            orderItem.setPrice(i.getUnitPrice());
            orderItem.setSubTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            subTotal = subTotal.add(orderItem.getSubTotal());

            // Update Flash Sale Sold Quantity if applicable
            this.flashSaleService.reserveSoldQuantity(
                    product.getId(),
                    (variant != null) ? variant.getId() : null,
                    i.getQuantity());

            order.addOrderItem(orderItem);
        }

        order.setTotalPrice(subTotal);

        // Shipping logic
        int shippingFeeRaw;
        if (req.getShippingFee() != null && req.getShippingFee() >= 0) {
            shippingFeeRaw = req.getShippingFee();
        } else {
            double weight = this.cartService.calculateTotalWeight(cartItems);
            shippingFeeRaw = this.shippingService.calculateShippingFee(address.getProvince(), address.getDistrict(),
                    address.getWard(), (int) weight);
        }
        BigDecimal shippingFee = (subTotal.compareTo(BigDecimal.valueOf(500000)) > 0) ? BigDecimal.ZERO
                : BigDecimal.valueOf(shippingFeeRaw);
        order.setShippingFee(shippingFee.intValue());

        // Coupon logic
        String couponCode = req.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            // Use CouponService to validate all complex rules (First order, Subscriber only, etc.)
            Coupon coupon = this.couponService.validateCoupon(couponCode, email);

            if (coupon.getMinOrderValue() != null && subTotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new IdInvalidException("Đơn hàng chưa đủ giá trị tối thiểu để áp dụng mã.");
            }

            // Check if user has this coupon in their wallet and if it's already used
            Optional<UserCoupon> userCouponOpt = this.userCouponRepository.findByUserAndCoupon(user, coupon);
            if (userCouponOpt.isPresent() && userCouponOpt.get().isUsed()) {
                throw new IdInvalidException("Bạn đã sử dụng mã giảm giá này cho đơn hàng khác.");
            }

            BigDecimal discountAmount = calculateDiscount(coupon, subTotal);
            order.setDiscountPrice(discountAmount);
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            this.couponRepository.save(coupon);

            // Mark UserCoupon as used (or create one if it's a public coupon used for the first time)
            UserCoupon userCoupon;
            if (userCouponOpt.isPresent()) {
                userCoupon = userCouponOpt.get();
            } else {
                userCoupon = new UserCoupon();
                userCoupon.setUser(user);
                userCoupon.setCoupon(coupon);
            }
            userCoupon.setUsed(true);
            userCoupon.setUsedAt(Instant.now());
            this.userCouponRepository.save(userCoupon);
        }

        BigDecimal finalPrice = subTotal.add(shippingFee).subtract(order.getDiscountPrice());
        order.setFinalPrice(finalPrice);
        Order savedOrder = this.orderRepository.save(order);

        // Payment Handling (Synchronous DB Record Creation)
        switch (paymentMethod) {
            case VNPAY:
                this.paymentService.createPendingVNPayPayment(savedOrder);
                break;
            case PAYOS:
                this.paymentService.createPendingPayOSPayment(savedOrder);
                break;
            case COD:
            default:
                this.paymentService.createCODPayment(savedOrder);
                break;
        }

        // Finalize order to save payment association
        savedOrder = this.orderRepository.save(savedOrder);

        // Only clear cart immediately for COD. For online payments, we clear after success callback.
        if (paymentMethod == PaymentMethodEnum.COD) {
            this.cartItemRepository.deleteAll(cartItems);
        }

        // Initialize lazy collections for Async Email processing
        this.forceLoadOrder(savedOrder);
        return savedOrder;
    }

    @Retryable(
        value = { CannotAcquireLockException.class, ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 500)
    )
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public ResGetOrderDTO createOrder(ReqCheckoutDTO req, HttpServletRequest request) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);

        // 1. PHASE 1: DB Transaction (Commits instantly, releasing connection & table locks)
        Order savedOrder = self.createOrderTransaction(req, email);

        return finalizeCreatedOrder(req, request, savedOrder);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public ResGetOrderDTO createOrderAsync(ReqCheckoutDTO req, String email) throws IdInvalidException {
        Order savedOrder = self.createOrderTransaction(req, email);
        return finalizeCreatedOrder(req, null, savedOrder);
    }

    private ResGetOrderDTO finalizeCreatedOrder(ReqCheckoutDTO req, HttpServletRequest request, Order savedOrder)
            throws IdInvalidException {
        // 2. PHASE 2: Slow external HTTP/Network Calls (Executed OUTSIDE database transaction)
        String paymentUrl = null;
        PaymentMethodEnum paymentMethod = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethodEnum.COD;
        switch (paymentMethod) {
            case VNPAY:
                ResPaymentVNPAYDTO vnpayRes = request != null
                        ? this.paymentService.createVnPayPayment(request, savedOrder.getPayment().getId())
                        : this.paymentService.createVnPayPayment("127.0.0.1", savedOrder.getPayment().getId());
                if (!"00".equals(vnpayRes.getCode())) {
                    throw new IdInvalidException("Lỗi khởi tạo thanh toán VNPay: " + vnpayRes.getMessage());
                }
                paymentUrl = vnpayRes.getPaymentUrl();
                break;
            case PAYOS:
                try {
                    String payosUrl = this.payOSService.createPaymentLink(savedOrder);
                    paymentUrl = payosUrl;
                } catch (Exception e) {
                    log.error(">>> PayOS payment link creation failed for order #{}", savedOrder.getId(), e);
                    throw new IdInvalidException("Lỗi khởi tạo thanh toán PayOS: " + e.getMessage());
                }
                break;
            case COD:
            default:
                break;
        }

        // 3. PHASE 3: Async Notifications & Emails
        boolean isCod = paymentMethod == PaymentMethodEnum.COD;
        this.emailService.sendOrderConfirmationEmail(savedOrder, isCod);

        this.notificationService.createNotification(
                savedOrder.getUser(),
                "Đặt hàng thành công",
                "Đơn hàng #" + savedOrder.getId() + " của bạn đã được tiếp nhận và đang chờ xử lý.",
                "ORDER_SUCCESS");

        // Send Telegram Notification to Admin (Only COD immediately)
        if (paymentMethod == PaymentMethodEnum.COD) {
            this.telegramService.sendOrderNotification(savedOrder);
        }

        // 4. PHASE 4: Build Response DTO
        ResGetOrderDTO resDTO = this.convertToResGetOrderDTO(savedOrder);
        if (paymentUrl != null) {
            resDTO.setPaymentUrl(paymentUrl);
        }
        resDTO.setPaymentMethod(paymentMethod.name());

        return resDTO;
    }

    public Order handleConfirmOrder(String token) throws IdInvalidException {
        Order order = this.orderRepository.findByConfirmationToken(token);
        if (order == null) {
            throw new IdInvalidException("Mã xác thực không hợp lệ hoặc đơn hàng không tồn tại.");
        }

        if (order.getStatus() == OrderStatusEnum.CANCELLED) {
            throw new IdInvalidException("Đơn hàng này đã bị hủy, không thể xác nhận.");
        }

        if (order.getStatus() == OrderStatusEnum.CONFIRMED) {
            return order; // Already confirmed
        }

        order.setStatus(OrderStatusEnum.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        order.setConfirmationToken(null); // Clear token after use
        
        Order savedOrder = this.orderRepository.save(order);
        
        // Send Thank You Email for COD Confirmation
        this.forceLoadOrder(savedOrder);
        this.emailService.sendOrderSuccessEmail(savedOrder);
        
        return savedOrder;
    }

    public Order getOrder(Long id) {
        Optional<Order> orderOptional = this.orderRepository.findById(id);
        return orderOptional.isPresent() ? orderOptional.get() : null;
    }

    public ResGetOrderDTO convertToResGetOrderDTO(Order order) {
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
        res.setProvince(order.getProvince());
        res.setDistrict(order.getDistrict());
        res.setWard(order.getWard());
        res.setPaymentStatus(order.getPaymentStatus());
        res.setShippingAddress(order.getShippingAddress());

        if (order.getPayment() != null) {
            res.setTransactionID(order.getPayment().getTransactionId());
            if (order.getPayment().getMethod() != null) {
                res.setPaymentMethod(order.getPayment().getMethod().name());
            }
        }
        res.setCreatedAt(order.getCreatedAt());
        res.setConfirmedAt(order.getConfirmedAt());
        res.setShippingCode(order.getShippingCode());
        res.setDeliveredAt(order.getDeliveredAt());

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
            if (coupon.getMaxDiscountValue() != null && coupon.getMaxDiscountValue().compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(coupon.getMaxDiscountValue()) > 0) {
                discount = coupon.getMaxDiscountValue();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        return discount;
    }

    @Transactional
    public Order handleUpdateStatus(Long id, OrderStatusEnum status, String reason) throws IdInvalidException {
        Order order = this.getOrder(id);
        if (order == null) {
            throw new IdInvalidException("Order not found with id: " + id);
        }

        order.setStatus(status);
        order = this.orderRepository.save(order);

        // Broadcast real-time update to Admin dashboard
        this.messagingTemplate.convertAndSend("/topic/order-updates", this.convertToResGetOrderDTO(order));

        // Gửi thông báo tự động dựa trên trạng thái mới
        String title = "";
        String message = "";
        String type = "ORDER_STATUS_UPDATE";

        if (status == OrderStatusEnum.CANCELLED && reason != null && !reason.isEmpty()) {
            order.setCancelReason(reason);
        }

        switch (status) {
            case CONFIRMED:
                title = "Đơn hàng đã được xác nhận";
                message = "Đơn hàng #" + order.getId() + " của bạn đã được xác nhận.";
                // Commit stock + cập nhật soldCount cho từng sản phẩm
                for (OrderItem item : order.getItems()) {
                    this.inventoryService.commitStock(
                            item.getProduct().getId(),
                            item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                            item.getQuantity(),
                            "Xác nhận đơn hàng #" + order.getId());
                    // Chỉ tính "đã bán" khi đơn hàng được xác nhận
                    item.getProduct().setSoldCount(item.getProduct().getSoldCount() + item.getQuantity());
                }

                // Send Telegram Notification to Admin
                this.telegramService.sendOrderConfirmedNotification(order);
                
                // Send Thank You Email (Admin manual confirmation)
                this.forceLoadOrder(order);
                this.emailService.sendOrderSuccessEmail(order);
                break;
            case DELIVERING:
                title = "Đơn hàng đang được giao";
                message = "Đơn hàng #" + order.getId() + " đang trên đường tới bạn.";
                break;
            case DELIVERED:
                title = "Giao hàng thành công";
                message = "Đơn hàng #" + order.getId() + " đã được giao thành công. Cảm ơn bạn!";
                
                // For COD, mark as PAID upon successful delivery
                if (order.getPayment() != null && 
                    order.getPayment().getMethod() == com.tuna.ecommerce.ultil.constant.PaymentMethodEnum.COD) {
                    order.setPaymentStatus(PaymentStatusEnum.PAID);
                    order.getPayment().setStatus(OrderStatusEnum.DELIVERED);
                }
                break;
            case CANCELLED:
                title = "Đơn hàng đã bị hủy";
                message = "Rất tiếc, đơn hàng #" + order.getId() + " của bạn đã bị hủy.";
                // Release stock + trừ soldCount
                for (OrderItem item : order.getItems()) {
                    this.inventoryService.releaseStock(
                            item.getProduct().getId(),
                            item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                            item.getQuantity(),
                            "Hủy đơn hàng #" + order.getId());
                    // Trừ soldCount (đảm bảo không âm)
                    int newSoldCount = Math.max(0, item.getProduct().getSoldCount() - item.getQuantity());
                    item.getProduct().setSoldCount(newSoldCount);
                }
                
                // Hoàn tiền VNPay
                if (order.getPaymentStatus() == PaymentStatusEnum.PAID && order.getPayment() != null 
                        && order.getPayment().getMethod() == com.tuna.ecommerce.ultil.constant.PaymentMethodEnum.VNPAY) {
                    boolean refundSuccess = this.paymentService.refundVNPayPayment(order.getPayment(), "System/Admin");
                    if (!refundSuccess) {
                        log.error("Failed to automatically refund VNPay for order ID: {}", order.getId());
                        // Có thể lưu note để nhân viên kiểm tra lại thủ công
                    }
                }
                
                // Hủy link PayOS nếu chưa thanh toán (để khách không quét mã được nữa)
                if (order.getPaymentStatus() != PaymentStatusEnum.PAID && order.getPayment() != null 
                        && order.getPayment().getMethod() == com.tuna.ecommerce.ultil.constant.PaymentMethodEnum.PAYOS) {
                    boolean cancelSuccess = this.payOSService.cancelPaymentLink(order.getId(), reason);
                    if (!cancelSuccess) {
                        log.warn("Failed to automatically cancel PayOS link for order ID: {}", order.getId());
                    }
                }
                break;
            default:
                break;
        }

        if (!title.isEmpty()) {
            this.notificationService.createNotification(order.getUser(), title, message, type);
        }
        
        if (status == OrderStatusEnum.CANCELLED) {
            this.emailService.sendOrderCancellationEmail(order);
        }

        return this.orderRepository.save(order);
    }

    public Order cancelOrder(Long id, String reason) throws IdInvalidException {
        Order order = this.getOrder(id);
        if (order == null) {
            throw new IdInvalidException("Đơn hàng không tồn tại.");
        }

        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        User user = this.userService.findByUsername(email);

        // Verify ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IdInvalidException("Bạn không có quyền hủy đơn hàng này.");
        }

        // Verify status
        boolean canCancel = false;
        if (order.getStatus() == OrderStatusEnum.PENDING) {
            canCancel = true;
        } else if (order.getStatus() == OrderStatusEnum.CONFIRMED && 
                   order.getPayment() != null && 
                   order.getPayment().getMethod() == com.tuna.ecommerce.ultil.constant.PaymentMethodEnum.VNPAY) {
            canCancel = true;
        }

        if (!canCancel) {
            throw new IdInvalidException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Chờ xử lý hoặc Đã xác nhận (đối với thanh toán VNPay).");
        }

        order.setCancelReason(reason);
        // Reuse handleUpdateStatus to handle stock release and notifications
        return this.handleUpdateStatus(order.getId(), OrderStatusEnum.CANCELLED, reason);
    }

    public void handleBulkUpdateStatus(List<Long> ids, OrderStatusEnum status, String reason) throws IdInvalidException {
        for (Long id : ids) {
            this.handleUpdateStatus(id, status, reason);
        }
    }

    public void handleClearCart(Order order) {
        User user = order.getUser();
        if (user != null && user.getCart() != null) {
            Long cartId = user.getCart().getId();
            for (OrderItem item : order.getItems()) {
                Long variantId = (item.getProductVariant() != null) ? item.getProductVariant().getId() : null;
                this.cartItemRepository.deleteByCartProductAndVariant(
                        cartId,
                        item.getProduct().getId(),
                        variantId);
            }
        }
    }

    @Transactional(readOnly = true)
    public String getOrdersSummaryForChatbot() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null)
            return "Người dùng chưa đăng nhập. Hãy khuyên khách hàng đăng nhập để xem đơn hàng.";

        User user = this.userService.findByUsername(email);
        if (user == null)
            return "Không tìm thấy thông tin người dùng.";

        // Lấy 5 đơn hàng gần nhất
        Page<Order> pageOrder = this.orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(),
                PageRequest.of(0, 5));
        List<Order> orders = pageOrder.getContent();

        if (orders.isEmpty()) {
            return "Khách hàng chưa có đơn hàng nào tại cửa hàng.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());

        StringBuilder sb = new StringBuilder("Thông tin 5 đơn hàng gần nhất của khách hàng:\n");
        for (Order o : orders) {
            sb.append("- Đơn hàng #").append(o.getId())
                    .append(": Trạng thái [").append(o.getStatus()).append("]")
                    .append(", Thanh toán: ").append(String.format("%,.0f VNĐ", o.getFinalPrice().doubleValue()))
                    .append(", Ngày đặt: ")
                    .append(o.getCreatedAt() != null ? formatter.format(o.getCreatedAt()) : "N/A")
                    .append(", Sản phẩm: ");
            if (o.getItems() != null && !o.getItems().isEmpty()) {
                o.getItems().forEach(oi -> sb.append(oi.getProduct().getName())
                        .append(" (SL: ").append(oi.getQuantity()).append("), "));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void handleDeleteOrder(Long id) throws IdInvalidException {
        Order order = this.getOrder(id);
        if (order == null) {
            return;
        }

        // 1. Release stock back to available before deleting
        for (OrderItem item : order.getItems()) {
            this.inventoryService.releaseStock(
                    item.getProduct().getId(),
                    item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                    item.getQuantity(),
                    "Xóa đơn hàng lỗi thanh toán #" + order.getId());
        }

        // 2. Identify payment to delete
        Long paymentId = (order.getPayment() != null) ? order.getPayment().getId() : null;

        // 3. Delete order (cascade will handle OrderItems)
        this.orderRepository.delete(order);

        // 4. Delete payment record separately if exists
        if (paymentId != null) {
            this.paymentRepository.deleteById(paymentId);
        }
    }
    public ResGetOrderDTO createGhnOrder(Long id) throws IdInvalidException {
        Order order = this.getOrder(id);
        if (order == null) {
            throw new IdInvalidException("Order not found");
        }
        if (order.getShippingCode() != null && !order.getShippingCode().isEmpty()) {
            throw new IdInvalidException("Order already has a shipping code: " + order.getShippingCode());
        }

        String shippingCode = this.shippingService.createShippingOrder(order);
        order.setShippingCode(shippingCode);
        this.orderRepository.save(order); Order updatedOrder = this.handleUpdateStatus(order.getId(), OrderStatusEnum.DELIVERING, null);
        return this.convertToResGetOrderDTO(updatedOrder);
    }

    public List<ResGetOrderDTO> handleBulkCreateGhnOrders(List<Long> ids) {
        List<ResGetOrderDTO> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                results.add(this.createGhnOrder(id));
            } catch (Exception e) {
                // Log error but continue with other orders
                log.error("Failed to create GHN order for ID {}: {}", id, e.getMessage());
            }
        }
        return results;
    }

    public Order handleUpdateOrderAddress(Long id, ReqUpdateOrderAddressDTO req) throws IdInvalidException {
        Order order = this.getOrder(id);
        if (order == null) {
            throw new IdInvalidException("Đơn hàng không tồn tại.");
        }

        order.setReceiverName(req.getReceiverName());
        order.setPhone(req.getPhone());
        order.setProvince(req.getProvince());
        order.setDistrict(req.getDistrict());
        order.setWard(req.getWard());
        order.setShippingAddress(req.getShippingAddress());

        order = this.orderRepository.save(order);

        // Broadcast real-time update to Admin dashboard
        this.messagingTemplate.convertAndSend("/topic/order-updates", this.convertToResGetOrderDTO(order));

        return order;
    }

    @Transactional(readOnly = true)
    public String getPurchaseHistorySummaryForChatbot() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) return "Chưa đăng nhập.";

        User user = this.userService.findByUsername(email);
        if (user == null) return "Không thấy User.";

        // Tìm tất cả đơn hàng đã DELIVERED
        List<Order> deliveredOrders = this.orderRepository.findAll().stream()
                .filter(o -> o.getUser().getId().equals(user.getId()) && o.getStatus() == OrderStatusEnum.DELIVERED)
                .collect(Collectors.toList());

        if (deliveredOrders.isEmpty()) return "Khách hàng chưa hoàn thành đơn hàng nào trước đây.";

        java.util.Set<String> productNames = new java.util.HashSet<>();
        for (Order o : deliveredOrders) {
            if (o.getItems() != null) {
                o.getItems().forEach(oi -> productNames.add(oi.getProduct().getName()));
            }
        }

        return "Các sản phẩm khách đã từng mua và sử dụng: " + String.join(", ", productNames);
    }

    public void forceLoadOrder(Order order) {
        Order managed = this.orderRepository.findCheckoutDetailsById(order.getId()).orElse(null);
        if (managed == null) return;

        // Force load on managed entity (session is active)
        if (managed.getUser() != null) {
            managed.getUser().getEmail();
        }
        if (managed.getItems() != null) {
            for (OrderItem item : managed.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().getName();
                    if (item.getProduct().getImages() != null) {
                        item.getProduct().getImages().size();
                    }
                }
                if (item.getProductVariant() != null) {
                    item.getProductVariant().getSku();
                    if (item.getProductVariant().getAttributeValues() != null) {
                        item.getProductVariant().getAttributeValues().size();
                    }
                }
            }
        }
        if (managed.getPayment() != null) {
            managed.getPayment().getMethod();
        }

        // Copy initialized references back to the original order object
        // so callers holding the old reference get the initialized data
        order.setUser(managed.getUser());
        order.setItems(managed.getItems());
        order.setPayment(managed.getPayment());
    }
}


