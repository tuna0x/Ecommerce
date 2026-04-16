package com.tuna.ecommerce.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserBehavior;
import com.tuna.ecommerce.domain.request.chat.ChatMessageDTO;
import com.tuna.ecommerce.repository.UserBehaviorRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    public enum ChatIntent {
        PRODUCT_SEARCH,        // tìm sản phẩm
        PRODUCT_DETAIL,        // hỏi chi tiết sản phẩm
        PRODUCT_RECOMMEND,     // gợi ý sản phẩm
        
        CART_INQUIRY,          // Thao tác/hỏi về giỏ hàng
        
        ORDER_STATUS,          // kiểm tra đơn
        ORDER_HISTORY,         // lịch sử đơn
        ORDER_CANCEL_REQUEST,  // hỏi hủy đơn (chỉ hướng dẫn)
        
        COUPON_LIST,           // xem mã giảm giá
        COUPON_APPLY,          // hỏi cách dùng mã
        
        USER_GREETING,         // chào hỏi
        SMALL_TALK,            // nói chuyện linh tinh
        COMPLAINT,             // phàn nàn
        UNKNOWN                // không hiểu
    }

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ProductService productService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiService(ProductService productService,
            OrderService orderService,
            CouponService couponService,
            CartService cartService,
            UserRepository userRepository,
            UserBehaviorRepository userBehaviorRepository,
            TrackingService trackingService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.productService = productService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.userBehaviorRepository = userBehaviorRepository;
        this.trackingService = trackingService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    private String formatBehaviorMetadata(UserBehavior b) {
        try {
            JsonNode meta = objectMapper.readTree(b.getMetadata());
            String action = b.getActionType().name();
            switch (action) {
                case "VIEW_PRODUCT":
                    return "Xem sản phẩm " + meta.path("productName").asText() + " (" + meta.path("price").asLong() + "đ)";
                case "ADD_CART":
                    return "Thêm " + meta.path("productName").asText() + " vào giỏ (SL: " + meta.path("quantity").asInt() + ")";
                case "UPDATE_CART":
                    return "Cập nhật giỏ hàng: " + meta.path("productName").asText() + " (SL: " + meta.path("newQuantity").asInt() + ")";
                case "REMOVE_CART":
                    return "Xóa khỏi giỏ: " + meta.path("productName").asText();
                case "SEARCH":
                    return "Tìm kiếm từ khóa: \"" + meta.path("keyword").asText() + "\" (" + meta.path("resultCount").asInt() + " kết quả)";
                case "VIEW_CATEGORY":
                    return "Xem danh mục: " + meta.path("categoryName").asText();
                case "USE_COUPON":
                    return "Sử dụng mã giảm giá: " + meta.path("couponCode").asText() + " (Giảm " + meta.path("discountAmount").asLong() + "đ)";
                case "BEGIN_CHECKOUT":
                    return "Bắt đầu thanh toán đơn hàng " + meta.path("cartTotal").asLong() + "đ";
                case "CHAT_WITH_BOT":
                    return "Đã hỏi AI: \"" + meta.path("messagePreview").asText() + "\"";
                case "TIME_ON_PAGE":
                    return "Dừng lại ở " + meta.path("path").asText() + " trong " + (meta.path("durationMs").asLong() / 1000) + " giây";
                default:
                    return action + ": " + b.getMetadata();
            }
        } catch (Exception e) {
            return b.getActionType().name() + " (Dữ liệu lỗi)";
        }
    }

    private ChatIntent determineIntent(String query) {
        if (query == null || query.trim().isEmpty()) return ChatIntent.UNKNOWN;
        String q = query.toLowerCase();

        // 1. Phàn nàn
        if (q.contains("tồi") || q.contains("kém") || q.contains("chậm") || q.contains("lâu") || q.contains("thất vọng") || q.contains("bực") || q.contains("tệ")) {
            return ChatIntent.COMPLAINT;
        }

        // 2. Liên quan đến đơn hàng
        if (q.contains("hủy")) return ChatIntent.ORDER_CANCEL_REQUEST;
        if (q.contains("lịch sử") && (q.contains("đơn") || q.contains("mua"))) return ChatIntent.ORDER_HISTORY;
        if (q.contains("đơn") || q.contains("giao hàng") || q.contains("vận chuyển") || q.contains("tình trạng") || q.contains("bill") || q.contains("chưa nhận")) {
            return ChatIntent.ORDER_STATUS;
        }

        // 3. Liên quan đến giỏ hàng
        if (q.contains("giỏ hàng") || q.contains("giỏ")) {
            return ChatIntent.CART_INQUIRY;
        }

        // 4. Liên quan đến mã giảm giá
        if (q.contains("cách dùng") && (q.contains("mã") || q.contains("voucher") || q.contains("coupon"))) return ChatIntent.COUPON_APPLY;
        if (q.contains("mã") || q.contains("giảm giá") || q.contains("khuyến mãi") || q.contains("voucher") || q.contains("sale") || q.contains("flashsale") || q.contains("freeship")) {
            return ChatIntent.COUPON_LIST;
        }

        // 5. Chào hỏi
        if (q.equals("chào") || q.equals("hi") || q.equals("hello") || q.equals("alo") || q.contains("có ai không")) {
            return ChatIntent.USER_GREETING;
        }

        // 6. Small Talk / Trò chuyện linh tinh
        if (q.contains("thời tiết") || q.contains("tên là gì") || q.contains("khỏe không") || q.contains("haha") || q.contains("đẹp không")) {
            return ChatIntent.SMALL_TALK;
        }

        // 7. Liên quan đến Sản phẩm
        if (q.contains("chi tiết") || q.contains("thành phần") || q.contains("hướng dẫn sử dụng") || q.contains("loại da")) return ChatIntent.PRODUCT_DETAIL;
        if (q.contains("tư vấn") || q.contains("gợi ý") || q.contains("nên mua") || q.contains("bán chạy") || q.contains("nào tốt") || q.contains("phù hợp")) return ChatIntent.PRODUCT_RECOMMEND;
        if (q.contains("tìm") || q.contains("có bán") || q.contains("giá") || q.contains("bao nhiêu") || q.contains("sản phẩm")) return ChatIntent.PRODUCT_SEARCH;

        return ChatIntent.UNKNOWN;
    }

    public String getChatResponse(String userMessage, List<ChatMessageDTO> history, String sessionId, String deviceType, String pageUrl) {
        int maxRetries = 2; // Thử lại tối đa 2 lần nếu gặp lỗi 503
        int retryCount = 0;

        ChatIntent intent = determineIntent(userMessage);

        while (retryCount <= maxRetries) {
            try {
                String url = apiUrl + "?key=" + apiKey;

                // Dynamically fetch Contexts based on Intent to save tokens
                String productContext = "";
                String orderContext = "";
                String couponContext = "";
                String cartContext = "";
                
                String expertKnowledgeContext = 
                    "--- KIẾN THỨC CHUYÊN GIA DA LIỄU ---\n" +
                    "1. LOẠI DA & CÁCH CHĂM SÓC:\n" +
                    "- DA DẦU (OILY): Lỗ chân lông to, bóng nhờn. Cần: SRM dạng gel, thành phần BHA (Salicylic Acid), Niacinamide, dưỡng ẩm mỏng nhẹ.\n" +
                    "- DA KHÔ (DRY): Bong tróc, căng rát. Cần: SRM dạng cream, thành phần Hyaluronic Acid (HA), Ceramides, Vitamin E, khóa ẩm kỹ.\n" +
                    "- DA NHẠY CẢM (SENSITIVE): Dễ đỏ, châm chích. Cần: Thành phần dịu nhẹ như Rau má (Centella), B5 (Panthenol), tránh hương liệu/cồn.\n" +
                    "- DA HỖN HỢP (COMBINATION): Dầu vùng chữ T, khô vùng má. Cần: Chăm sóc vùng T như da dầu và vùng má như da khô.\n" +
                    "2. QUY TRÌNH (ROUTINE) CHUẨN:\n" +
                    "- SÁNG: SRM -> Toner -> Serum -> Dưỡng ẩm -> CHỐNG NẮNG (Bắt buộc).\n" +
                    "- TỐI: Tẩy trang -> SRM -> Tẩy da chết (2-3 lần/tuần) -> Toner -> Serum -> Dưỡng ẩm (Khóa ẩm).\n" +
                    "3. THÔNG TIN GIAO HÀNG & THANH TOÁN:\n" +
                    "- PHƯƠNG THỨC THANH TOÁN: Hỗ trợ [1] Thanh toán khi nhận hàng (COD) và [2] Thanh toán trực tuyến qua VNPAY (Thẻ ngân hàng, Ví điện tử, QR Code).\n" +
                    "- PHÍ VẬN CHUYỂN: Miễn phí toàn quốc cho đơn từ 500k. Đơn dưới 500k phí ship đồng giá 30k.\n" +
                    "- THỜI GIAN GIAO HÀNG: Nội thành Hà Nội/TP.HCM (1-2 ngày), các tỉnh thành khác (2-4 ngày).\n" +
                    "4. CHÍNH SÁCH BÔNG COSMETIC:\n" +
                    "- BAO CHECK: Cam kết chính hãng 100%, phát hiện giả đền 200%.\n" +
                    "- ĐỔI TRẢ: 7 ngày đầu nếu có lỗi sản phẩm hoặc dị ứng/kích ứng có xác nhận.\n" +
                    "5. CÂU CHUYỆN THƯƠNG HIỆU:\n" +
                    "- Bông Cosmetic ra đời từ 2020 với sứ mệnh mang lại vẻ đẹp tự nhiên, an toàn cho phụ nữ Việt qua các dòng mỹ phẩm chính hãng hàng đầu thế giới.";

                // Routing variables based on sophisticated 15 Intention Enum
                switch (intent) {
                    case ORDER_STATUS:
                    case ORDER_HISTORY:
                    case ORDER_CANCEL_REQUEST:
                        orderContext = this.orderService.getOrdersSummaryForChatbot();
                        break;
                    case COUPON_LIST:
                    case COUPON_APPLY:
                        couponContext = this.couponService.getCouponsSummaryForChatbot();
                        // Lấy thêm sản phẩm vì khách có thể hỏi "sản phẩm nào đang sale/flashsale"
                        productContext = this.productService.getProductsSummaryForChatbot(userMessage);
                        break;
                    case PRODUCT_SEARCH:
                    case PRODUCT_DETAIL:
                    case PRODUCT_RECOMMEND:
                        productContext = this.productService.getProductsSummaryForChatbot(userMessage);
                        break;
                    case CART_INQUIRY:
                        // Giỏ hàng sẽ được load ở phía dưới (chung cho mọi request cần giỏ)
                        break;
                    case COMPLAINT:
                    case SMALL_TALK:
                    case USER_GREETING:
                        // No slow DB calls for general greetings or small talk
                        break;
                    case UNKNOWN:
                        // Fallback behavior: provide top-selling products as a suggestion
                        productContext = this.productService.getProductsSummaryForChatbot("");
                        break;
                }
                
                // User, Cart & Activity context
                String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
                String userNameContext = "Khách lạ (Chưa đăng nhập)";
                String userActivityContext = "";
                
                if (currentUserEmail != null) {
                    User user = userRepository.findByEmail(currentUserEmail);
                    if (user != null && user.getUserProfile() != null) {
                        userNameContext = user.getUserProfile().getName();
                    }
                    
                    Cart cart = cartService.getOrCreateCart();
                    if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
                        StringBuilder cartSb = new StringBuilder();
                        for (CartItem item : cart.getItems()) {
                            cartSb.append("- ").append(item.getProduct().getName())
                                  .append(" (Số lượng: ").append(item.getQuantity()).append(")\n");
                        }
                        cartContext = cartSb.toString();
                    } else {
                        cartContext = "Giỏ hàng trống.";
                    }
 
                    // Lấy 10 hành động gần nhất
                    List<UserBehavior> recentBehaviors = userBehaviorRepository.findTop10ByUserEmailOrderByCreatedAtDesc(currentUserEmail);
                    if (recentBehaviors != null && !recentBehaviors.isEmpty()) {
                        StringBuilder logSb = new StringBuilder();
                        for (UserBehavior b : recentBehaviors) {
                            logSb.append("- ").append(formatBehaviorMetadata(b))
                                 .append(" [Trang: ").append(b.getPageUrl() != null ? b.getPageUrl() : "N/A").append("]\n");
                        }
                        userActivityContext = logSb.toString();
                    }
                }

                String currentEnvironment = "Người dùng đang sử dụng thiết bị: " + (deviceType != null ? deviceType : "Không rõ") + 
                                           "\nTrang hiện tại đang xem: " + (pageUrl != null ? pageUrl : "Trang chủ");
                if (sessionId != null) {
                    currentEnvironment += "\nSession ID: " + sessionId;
                }

                // Prepare Payload for Gemini 2.5
                Map<String, Object> requestBody = new HashMap<>();

                // System instructions (context)
                Map<String, Object> systemInstructionMap = new HashMap<>();
                Map<String, String> systemPart = new HashMap<>();
                systemPart.put("text", "Bạn là 'Bông', Chuyên gia tư vấn da liễu dẻo miệng của thương hiệu Bông Cosmetic. " +
                        "Hãy gọi khách là 'Nàng/Cậu' nếu không biết tên, hiện tại bạn đang chat với: [" + userNameContext + "]. " +
                        "Hãy chủ động chào tên thật của khách để tạo sự thân thiết.\n" +
                        "TUYỆT ĐỐI TUÂN THỦ: NẾU KHÁCH HỎI MÀ TRONG DỮ LIỆU ĐƯỢC CUNG CẤP BÊN DƯỚI KHÔNG CÓ, HÃY XIN LỖI KHÉO LÉO VÀ BÁO RẰNG CHƯA TÌM THẤY. KHÔNG ĐƯỢC BỊA ĐẶT HOẶC MÔ PHỎNG DỮ LIỆU GIẢ!" +
                        (productContext.isEmpty() ? "" : "\n\n--- DỮ LIỆU SẢN PHẨM HIỆN CÓ TRONG CỬA HÀNG ---\n" + productContext) +
                        (couponContext.isEmpty() ? "" : "\n\n--- DỮ LIỆU VOUCHER KHUYẾN MÃI ---\n" + couponContext) +
                        (orderContext.isEmpty() ? "" : "\n\n--- DỮ LIỆU ĐƠN HÀNG CỦA KHÁCH ---\n" + orderContext) +
                        "\n\n--- DỮ LIỆU GIỎ HÀNG HIỆN TẠI ---\n" + cartContext +
                        (userActivityContext.isEmpty() ? "" : "\n\n--- LỊCH SỬ HÀNH ĐỘNG GẦN NHẤT CỦA KHÁCH ---\n" + userActivityContext) +
                        "\n\n--- THÔNG TIN PHIÊN HIỆN TẠI ---\n" + currentEnvironment +
                        "\n\n" + expertKnowledgeContext +
                        "\n\nNHIỆM VỤ CỦA CHUYÊN GIA BÔNG:\n" +
                        "1. Tư vấn chuyên sâu: Khi khách hỏi về vấn đề da hoặc skincare, hãy dựa vào kiến thức chuyên gia bên trên để phân tích và ĐỀ XUẤT SẢN PHẨM CÓ TRONG CỬA HÀNG (Sử dụng Markdown [Tên sản phẩm](/product/ID)).\n" +
                        "2. Chủ động hỏi: Nếu khách chưa nói rõ loại da, hãy khéo léo hỏi 'Da nàng là da gì nhỉ?' để tư vấn Routine chuẩn xác.\n" +
                        "3. Soi GIỎ HÀNG và LỊCH SỬ: Gợi ý các món bổ trợ để hoàn thiện Routine dựa vào sản phẩm họ vừa xem/tương tác.\n" +
                        "4. Đơn hàng: Nếu khách muốn mua tiếp thì khuyên, nếu hủy thì báo khách bấm vào mục Tài khoản -> Đơn hàng (bạn ko có quyền hủy).\n" +
                        "5. Luôn dùng thái độ rạng rỡ (🌸💖) và xin lỗi chân thành nếu không tìm thấy món độ/đơn hàng khách cần."
                );
                systemInstructionMap.put("parts", List.of(systemPart));
                requestBody.put("system_instruction", systemInstructionMap);

                // Build Conversation Contents (History + Current Message)
                List<Map<String, Object>> contents = new ArrayList<>();

                // Add History
                if (history != null) {
                    for (ChatMessageDTO msg : history) {
                        Map<String, Object> contentPart = new HashMap<>();
                        Map<String, String> part = new HashMap<>();
                        part.put("text", msg.getContent());
                        contentPart.put("role", msg.getRole().equals("assistant") ? "model" : "user");
                        contentPart.put("parts", List.of(part));
                        contents.add(contentPart);
                    }
                }

                // Add Current User Message
                Map<String, Object> contentUser = new HashMap<>();
                Map<String, String> userPart = new HashMap<>();
                userPart.put("text", userMessage);
                contentUser.put("role", "user");
                contentUser.put("parts", List.of(userPart));
                contents.add(contentUser);

                requestBody.put("contents", contents);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
                String responseStr = responseEntity.getBody();

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    JsonNode root = objectMapper.readTree(responseStr);
                    String aiResponse = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

                    // Ghi log hành động chat từ backend
                    Map<String, Object> logMeta = new HashMap<>();
                    logMeta.put("userMessage", userMessage);
                    logMeta.put("aiIntent", intent);
                    logMeta.put("responsePreview", aiResponse.length() > 50 ? aiResponse.substring(0, 50) + "..." : aiResponse);
                    
                    trackingService.logActivity(
                        currentUserEmail != null ? currentUserEmail : "anonymous",
                        "server-side", // IP can be fetched from context if needed
                        "CHAT_WITH_BOT",
                        objectMapper.writeValueAsString(logMeta),
                        sessionId,
                        deviceType,
                        null,
                        pageUrl
                    );

                    return aiResponse;
                }

                return "Hệ thống đang bận, bạn vui lòng thử lại sau giây lát nhé! 🙏";

            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                // Lỗi 503 - Quá tải
                retryCount++;
                if (retryCount <= maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Đợi 1-2s rồi thử lại
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                return "Máy chủ AI của Google đang quá tải. Bạn hãy quay lại sau ít phút nhé! ☕";
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                return "Bạn đang gửi yêu cầu hơi nhanh, hãy đợi một chút rồi nhắn lại nhé! 🚀";
            } catch (Exception e) {
                System.err.println(">>> Gemini Error: " + e.getMessage());
                return "Rất tiếc, tôi đang gặp một chút sự cố kỹ thuật. Bạn có thể nhắn lại sau được không?";
            }
        }
        return "Xin lỗi, hiện tại tôi không thể kết nối tới máy chủ AI. Vui lòng thử lại sau.";
    }
}
