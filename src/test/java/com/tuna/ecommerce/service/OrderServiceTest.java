package com.tuna.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.tuna.ecommerce.domain.Address;
import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.repository.CartItemRepository;
import com.tuna.ecommerce.repository.CouponRepository;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.repository.PaymentRepository;
import com.tuna.ecommerce.repository.UserCouponRepository;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;
    @Mock
    private UserService userService;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private AddressService addressService;
    @Mock
    private ShippingService shippingService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private CouponService couponService;
    @Mock
    private TelegramService telegramService;
    @Mock
    private FlashSaleService flashSaleService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private PayOSService payOSService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Address testAddress;
    private Product testProduct;
    private CartItem testCartItem;
    private List<CartItem> testCartItems;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");

        testAddress = new Address();
        testAddress.setId(10L);
        testAddress.setReceiverName("Nguyen Van A");
        testAddress.setPhone("0987654321");
        testAddress.setProvince("Hà Nội");
        testAddress.setDistrict("Quận Cầu Giấy");
        testAddress.setWard("Phường Dịch Vọng");
        testAddress.setDetail("Số 1 Cầu Giấy");

        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setName("Sản phẩm dưỡng da cao cấp");

        Cart testCart = new Cart();
        testCart.setId(50L);
        testCart.setUser(testUser);

        testCartItem = new CartItem();
        testCartItem.setId(500L);
        testCartItem.setCart(testCart);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);
        testCartItem.setUnitPrice(BigDecimal.valueOf(150000));
        testCartItem.setTotalPrice(BigDecimal.valueOf(300000));

        testCartItems = new ArrayList<>();
        testCartItems.add(testCartItem);
    }

    @Test
    @DisplayName("Đặt hàng COD thành công với giỏ hàng hợp lệ")
    void createOrderTransaction_Success_COD() throws IdInvalidException {
        // Arrange
        ReqCheckoutDTO req = new ReqCheckoutDTO();
        req.setAddressId(10L);
        req.setCartItemId(Arrays.asList(500L));
        req.setPaymentMethod(PaymentMethodEnum.COD);
        req.setCouponCode(null);

        String email = "user@example.com";

        when(userService.findByUsername(email)).thenReturn(testUser);
        when(addressService.getAddressById(10L)).thenReturn(testAddress);
        when(cartItemRepository.findByIdIn(Arrays.asList(500L))).thenReturn(testCartItems);
        
        // Mock calculate total weight and shipping fee
        when(cartService.calculateTotalWeight(testCartItems)).thenReturn(0.5);
        when(shippingService.calculateShippingFee(eq("Hà Nội"), eq("Quận Cầu Giấy"), eq("Phường Dịch Vọng"), anyInt()))
            .thenReturn(30000);

        // Mock Order Saving
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1000L); // set ID for the first save
            }
            return order;
        });

        // Act
        Order result = orderService.createOrderTransaction(req, email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1000L);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getReceiverName()).isEqualTo("Nguyen Van A");
        assertThat(result.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(result.getShippingFee()).isEqualTo(30000);
        assertThat(result.getFinalPrice()).isEqualByComparingTo(BigDecimal.valueOf(330000)); // 300k + 30k ship

        // Verify interactions
        verify(inventoryService, times(1)).reserveStock(100L, null, 2);
        verify(flashSaleService, times(1)).incrementSoldQuantity(100L, 2);
        verify(paymentService, times(1)).createCODPayment(1000L);
        verify(cartItemRepository, times(1)).deleteAll(testCartItems);
    }

    @Test
    @DisplayName("Đặt hàng thất bại do Địa chỉ không tồn tại")
    void createOrderTransaction_Fail_AddressNotFound() {
        // Arrange
        ReqCheckoutDTO req = new ReqCheckoutDTO();
        req.setAddressId(99L); // Invalid Address ID
        req.setCartItemId(Arrays.asList(500L));
        req.setPaymentMethod(PaymentMethodEnum.COD);

        String email = "user@example.com";

        when(userService.findByUsername(email)).thenReturn(testUser);
        when(addressService.getAddressById(99L)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrderTransaction(req, email))
            .isInstanceOf(IdInvalidException.class)
            .hasMessageContaining("Địa chỉ không tồn tại.");
    }

    @Test
    @DisplayName("Đặt hàng thất bại do Giỏ hàng trống")
    void createOrderTransaction_Fail_CartEmpty() {
        // Arrange
        ReqCheckoutDTO req = new ReqCheckoutDTO();
        req.setAddressId(10L);
        req.setCartItemId(Arrays.asList(999L)); // Giỏ hàng rỗng/Không tìm thấy
        req.setPaymentMethod(PaymentMethodEnum.COD);

        String email = "user@example.com";

        when(userService.findByUsername(email)).thenReturn(testUser);
        when(addressService.getAddressById(10L)).thenReturn(testAddress);
        when(cartItemRepository.findByIdIn(Arrays.asList(999L))).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrderTransaction(req, email))
            .isInstanceOf(IdInvalidException.class)
            .hasMessageContaining("Giỏ hàng của bạn đang trống.");
    }

    @Test
    @DisplayName("Đặt hàng thất bại do Sản phẩm hết hàng trong kho")
    void createOrderTransaction_Fail_OutOfStock() throws IdInvalidException {
        // Arrange
        ReqCheckoutDTO req = new ReqCheckoutDTO();
        req.setAddressId(10L);
        req.setCartItemId(Arrays.asList(500L));
        req.setPaymentMethod(PaymentMethodEnum.COD);

        String email = "user@example.com";

        when(userService.findByUsername(email)).thenReturn(testUser);
        when(addressService.getAddressById(10L)).thenReturn(testAddress);
        when(cartItemRepository.findByIdIn(Arrays.asList(500L))).thenReturn(testCartItems);

        // Mock reserveStock ném ra lỗi hết hàng
        org.mockito.Mockito.doThrow(new IdInvalidException("Hết hàng"))
            .when(inventoryService).reserveStock(100L, null, 2);

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrderTransaction(req, email))
            .isInstanceOf(IdInvalidException.class)
            .hasMessageContaining("không đủ hàng");
    }
}

