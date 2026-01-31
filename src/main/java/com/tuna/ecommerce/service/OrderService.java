package com.tuna.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Coupon;
import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.request.order.ReqCreateOrderDTO;
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

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final CartItemRepository cartItemRepository;
    private final CouponRepository couponRepository;


    public Order createOder(ReqCheckoutDTO req) throws IdInvalidException{
        String email = this.securityUtil.getCurrentUserLogin().orElse(null);
        User user = this.userService.findByUsername(email);
        String couponCode=req.getCouponCode();

        List<CartItem> cartItems= cartItemRepository.findByIdIn(req.getCartItemId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("cart empty");
        }
        Order order= new Order();
        order.setUser(user);
        order.setDiscountPrice(BigDecimal.valueOf(0.0));
        order.setStatus(OrderStatusEnum.PENDDING);
        order.setShippingAddress(req.getShippingAdress());
        order.setPaymentStatus(PaymentStatusEnum.UNPAID);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total=BigDecimal.ZERO;

        for(CartItem i: cartItems){
            OrderItem orderItem=new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(i.getProduct());
            orderItem.setQuantity(i.getQuantity());
            orderItem.setPrice(i.getUnitPrice());
            orderItem.setSubTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            total=total.add(orderItem.getSubTotal());
            orderItems.add(orderItem);
        }
        order.setTotalPrice(total);
        order.setItems(orderItems);

        if (couponCode!=null && !couponCode.isBlank()) {
            Coupon coupon=this.couponRepository.findByCode(couponCode).orElseThrow(()-> new IdInvalidException("Coupon code not found"));
            if (coupon.getUsedCount()>coupon.getUsageLimit()) {
                throw new IdInvalidException("Coupon usage limit reached");
            }
            if (coupon.getMinOrderValue() !=null && order.getTotalPrice().compareTo(coupon.getMinOrderValue()) < 0 ) {
                throw new IdInvalidException("Coupon usage condition not met");
            }
            if (coupon.getEndDate().isBefore(LocalDateTime.now())) {
                throw new IdInvalidException("Coupon is expired");
            }
            BigDecimal discountPrice=calculateDiscount(coupon, order.getTotalPrice());
            order.setDiscountPrice(discountPrice);
            order.setFinalPrice(order.getTotalPrice().subtract(discountPrice));
            coupon.setUsedCount(coupon.getUsedCount()+1);
            this.couponRepository.save(coupon);
        }

        this.cartItemRepository.deleteAll(cartItems);

        return this.orderRepository.save(order);
    }

    public Order getOrder(Long id){
    Optional<Order> orderOptional= this.orderRepository.findById(id);
    return orderOptional.isPresent() ? orderOptional.get() : null;
    }

    public ResGetOrderDTO convertToResGetOderDTO(Order order){
        ResGetOrderDTO res=new ResGetOrderDTO();
        ResGetOrderDTO.UserInner userInner=new ResGetOrderDTO.UserInner();

        res.setId(order.getId());

        userInner.setId(order.getUser().getId());
        userInner.setName(order.getUser().getName());
        userInner.setEmail(order.getUser().getEmail());
        res.setUser(userInner);

        res.setStatus(order.getStatus());
        res.setTotalPrice(order.getFinalPrice());
        res.setPaymentStatus(order.getPaymentStatus());
        res.setShippingAddress(order.getShippingAddress());
        res.setTransactionID(order.getPayment().getTransactionId());
        return res;
    }

        private BigDecimal calculateDiscount(Coupon coupon, BigDecimal total) {
        return coupon.getType() == CouponTypeEnum.PERCENT
                ? coupon.getValue().multiply(total).divide(BigDecimal.valueOf(100))  : coupon.getValue();
    }
}
