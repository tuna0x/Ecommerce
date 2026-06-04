package com.tuna.ecommerce.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.Product;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserProfile;
import com.tuna.ecommerce.domain.UserBehavior;
import com.tuna.ecommerce.domain.ChatInteraction;
import com.tuna.ecommerce.domain.request.chat.ChatMessageDTO;
import com.tuna.ecommerce.repository.UserBehaviorRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.repository.ChatInteractionRepository;
import com.tuna.ecommerce.repository.UserProfileRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private enum ChatIntent {
        TOP_HOT_PRODUCTS,
        PRODUCT_SEARCH,
        FLASH_SALE,
        COUPON,
        CART,
        ORDER_STATUS,
        PURCHASE_HISTORY,
        CATEGORY_LIST,
        REVIEW_LOOKUP,
        SKINCARE_ROUTINE,
        BLOG_KNOWLEDGE,
        UNKNOWN
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
    private final UserProfileRepository userProfileRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final ChatInteractionRepository chatInteractionRepository;
    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TelegramService telegramService;
    private final BlogService blogService;
    private final CategoryService categoryService;
    private final FlashSaleService flashSaleService;
    private final ReviewService reviewService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public GeminiService(ProductService productService,
            OrderService orderService,
            CouponService couponService,
            CartService cartService,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserBehaviorRepository userBehaviorRepository,
            ChatInteractionRepository chatInteractionRepository,
            TrackingService trackingService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            TelegramService telegramService,
            BlogService blogService,
            CategoryService categoryService,
            FlashSaleService flashSaleService,
            ReviewService reviewService,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService) {
        this.productService = productService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userBehaviorRepository = userBehaviorRepository;
        this.chatInteractionRepository = chatInteractionRepository;
        this.trackingService = trackingService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.telegramService = telegramService;
        this.blogService = blogService;
        this.categoryService = categoryService;
        this.flashSaleService = flashSaleService;
        this.reviewService = reviewService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    public String getChatResponse(String userMessage, List<ChatMessageDTO> history, String sessionId, String deviceType,
            String pageUrl) {
        try {
            String url = apiUrl + "?key=" + apiKey;

            // 1. Phân tích ngữ cảnh người dùng
            String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
            StringBuilder context = new StringBuilder();
            String userName = "Nàng";
            String userSkinType = "Chưa rõ";

            if (currentUserEmail != null) {
                User user = userRepository.findByEmail(currentUserEmail);
                if (user != null && user.getUserProfile() != null) {
                    userName = user.getUserProfile().getName();
                    userSkinType = user.getUserProfile().getSkinType() != null ? user.getUserProfile().getSkinType()
                            : "Chưa rõ";
                }

                List<UserBehavior> behaviors = userBehaviorRepository
                        .findTop10ByUserEmailOrderByCreatedAtDesc(currentUserEmail);
                if (behaviors != null && !behaviors.isEmpty()) {
                    context.append("\n--- HÀNH VI GẦN ĐÂY ---\n");
                    Map<String, Long> actionCounts = behaviors.stream()
                            .collect(Collectors.groupingBy(b -> b.getActionType().toString(), Collectors.counting()));
                    context.append("- Hành động: ").append(actionCounts).append("\n");
                    String lastPages = behaviors.stream().map(UserBehavior::getPageUrl).distinct().limit(3)
                            .collect(Collectors.joining(", "));
                    context.append("- Trang xem: ").append(lastPages).append("\n");
                }
            }

            // 2. Định nghĩa Tools
            ChatIntent routedIntent = classifyIntent(userMessage);
            String deterministicResponse = buildDeterministicResponse(routedIntent, userMessage, userSkinType);
            if (deterministicResponse != null) {
                saveChatInteraction(currentUserEmail, userMessage, deterministicResponse, sessionId, deviceType,
                        pageUrl, routedIntent.name());
                logChatActivity(currentUserEmail, userMessage, deterministicResponse, sessionId, deviceType, pageUrl);
                return deterministicResponse;
            }

            if (isTopHotProductQuestion(userMessage)) {
                int limit = extractRequestedLimit(userMessage, 5);
                String aiResponse = productService.getTopProductsSummaryForChatbot(limit);
                saveChatInteraction(currentUserEmail, userMessage, aiResponse, sessionId, deviceType, pageUrl,
                        "TOP_HOT_PRODUCTS");
                logChatActivity(currentUserEmail, userMessage, aiResponse, sessionId, deviceType, pageUrl);
                return aiResponse;
            }

            List<Map<String, Object>> tools = List.of(Map.of("function_declarations", List.of(
                    Map.of("name", "tra_cuu_san_pham", "description",
                            "Tìm kiếm sản phẩm. Hãy đính kèm loại da của khách vào từ khóa để tìm chính xác hơn (Vd: 'kem chống nắng da dầu'). Kiểm tra kỹ trường 'Loại da' trong kết quả.",
                            "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("query", Map.of("type", "STRING")))),
                    Map.of("name", "kiem_tra_don_hang", "description", "Xem đơn hàng.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "tim_ma_giam_gia", "description", "Tìm mã giảm giá.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "xem_gio_hang", "description", "Xem giỏ hàng.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "xem_danh_muc", "description", "Xem danh muc san pham hien co trong cua hang.",
                            "parameters", Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "doc_bai_viet_blog", "description", "Tra cứu kiến thức blog.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("keyword", Map.of("type", "STRING")))),
                    Map.of("name", "tra_cuu_flash_sale", "description", "Xem Flash Sale.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "tra_cuu_lich_su_mua_hang", "description", "Xem lịch sử mua hàng.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "xem_danh_gia_san_pham", "description", "Xem review khách.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("productId", Map.of("type", "NUMBER")))),
                    Map.of("name", "cap_nhat_loai_da", "description",
                            "Cập nhật loại da vào hồ sơ khách hàng sau khi trắc nghiệm xong.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("skinType", Map.of("type", "STRING",
                                    "description", "Loại da xác định được (Vd: Da dầu, Da khô, Da nhạy cảm...)")))))));

            // 3. Huấn luyện Persona "Bông"
            String purchaseHistory = orderService.getPurchaseHistorySummaryForChatbot();
            String flashSaleSummary = flashSaleService.getFlashSaleSummaryForChatbot();
            String trainingCases = buildTrainingCasesPrompt();

            String systemPrompt = "Bạn là 'Bông' - Chuyên gia tư vấn da liễu và trợ lý mua sắm cá nhân của Bông Cosmetic.\n"
                    +
                    "KHÁCH HÀNG: " + userName + ".\n" +
                    "LOẠI DA: " + userSkinType + ".\n" +
                    "PHONG CÁCH: Ngọt ngào, tận tâm, dùng emoji (🌸, 💖, ✨).\n\n" +
                    "QUY TẮC VÀNG:\n" +
                    "1. THẤU HIỂU HÀNH VI: Dựa vào 'HÀNH VI GẦN ĐÂY' để đưa ra gợi ý trúng đích. Ví dụ nếu khách vừa xem nhiều kem chống nắng, hãy chủ động hỏi họ có muốn tìm loại phù hợp nhất không.\n"
                    +
                    "2. RICH CARDS (QUAN TRỌNG): Khi bạn giới thiệu bất kỳ sản phẩm nào, bạn BẮT BUỘC phải đính kèm thẻ tag định dạng: [PRODUCT_CARD:id|name|price|thumbnail] ngay sau tên sản phẩm đó. Thông tin này lấy từ công cụ 'tra_cuu_san_pham'.\n"
                    +
                    "3. QUICK REPLIES: Mỗi khi trả lời xong, hãy gợi ý 2-3 câu hỏi tiếp theo mà khách có thể muốn hỏi vào cuối tin nhắn theo định dạng: [QUICK_REPLY:Câu hỏi gợi ý 1|Câu hỏi gợi ý 2].\n"
                    +
                    "4. ƯU TIÊN LOẠI DA: Luôn gợi ý sản phẩm khớp với loại da '" + userSkinType + "'.\n" +
                    "5. FLASH SALE: Luôn cập nhật thông tin Flash Sale mới nhất: " + flashSaleSummary + ".\n" +
                    "6. GIẢI THÍCH LÝ DO: Khi gợi ý sản phẩm, hãy giải thích ngắn gọn tại sao sản phẩm đó phù hợp với tình trạng da của khách.\n\n"
                    +
                    trainingCases + "\n" +
                    "LỊCH SỬ MUA HÀNG CỦA KHÁCH: " + purchaseHistory + "\n\n" +
                    context.toString();

            Map<String, Object> systemInstruction = Map.of("parts", List.of(Map.of("text", systemPrompt)));

            // 4. Interaction Loop
            List<Map<String, Object>> contents = new ArrayList<>();
            if (history != null) {
                for (ChatMessageDTO msg : history) {
                    contents.add(Map.of("role", msg.getRole().equals("assistant") ? "model" : "user", "parts",
                            List.of(Map.of("text", msg.getContent()))));
                }
            }
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));

            String lastFunctionName = "NONE";
            for (int i = 0; i < 5; i++) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("system_instruction", systemInstruction);
                requestBody.put("contents", contents);
                requestBody.put("tools", tools);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, new HttpHeaders());
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                JsonNode candidate = root.path("candidates").get(0);
                JsonNode parts = candidate.path("content").path("parts").get(0);

                if (parts.has("functionCall")) {
                    JsonNode call = parts.get("functionCall");
                    lastFunctionName = call.get("name").asText();
                    String result = executeFunction(lastFunctionName, call.get("args"));
                    contents.add(Map.of("role", "model", "parts", List.of(parts)));
                    contents.add(Map.of("role", "function", "parts", List.of(Map.of("functionResponse",
                            Map.of("name", lastFunctionName, "response", Map.of("content", result))))));
                } else {
                    String aiResponse = parts.path("text").asText();
                    saveChatInteraction(currentUserEmail, userMessage, aiResponse, sessionId, deviceType, pageUrl,
                            lastFunctionName);
                    logChatActivity(currentUserEmail, userMessage, aiResponse, sessionId, deviceType, pageUrl);
                    return aiResponse;
                }
            }
            return "Bông đang suy nghĩ chút nhé... 🌸";
        } catch (Exception e) {
            e.printStackTrace();
            return "Bông gặp sự cố nhỏ, Nàng đợi chút nha! 💖";
        }
    }

    private String executeFunction(String name, JsonNode args) {
        switch (name) {
            case "tra_cuu_san_pham":
                return productService.getProductsSummaryForChatbot(args.path("query").asText());
            case "kiem_tra_don_hang":
                return orderService.getOrdersSummaryForChatbot();
            case "tim_ma_giam_gia":
                return couponService.getCouponsSummaryForChatbot();
            case "xem_gio_hang":
                return getCartSummary();
            case "xem_danh_muc":
                return categoryService.getCategoriesSummaryForChatbot();
            case "doc_bai_viet_blog":
                return blogService.getBlogsSummaryForChatbot(args.path("keyword").asText());
            case "tra_cuu_flash_sale":
                return flashSaleService.getFlashSaleSummaryForChatbot();
            case "tra_cuu_lich_su_mua_hang":
                return orderService.getPurchaseHistorySummaryForChatbot();
            case "xem_danh_gia_san_pham":
                return reviewService.getReviewsSummaryForChatbot(args.path("productId").asLong());
            case "cap_nhat_loai_da":
                return updateSkinType(args.path("skinType").asText());
            default:
                return "Không tìm thấy công cụ.";
        }
    }

    private String buildTrainingCasesPrompt() {
        return """
                --- CASE NGUOI DUNG HAY HOI VA CACH XU LY ---
                1. Tim san pham theo nhu cau hoac van de da:
                   Vi du: "da dau nen dung gi", "tim serum tri tham", "kem chong nang cho da nhay cam", "san pham duoi 300k".
                   Bat buoc goi tool tra_cuu_san_pham voi query gom nhu cau, loai da, muc gia neu co.
                2. Hoi top, hot, ban chay, noi bat:
                   Vi du: "top 2 san pham hot nhat", "my pham ban chay", "san pham duoc yeu thich".
                   Uu tien du lieu ban chay va luon kem PRODUCT_CARD.
                3. Hoi flash sale, giam gia, deal:
                   Vi du: "hom nay co flash sale khong", "san pham nao dang sale", "deal tot nhat".
                   Goi tool tra_cuu_flash_sale; neu co san pham thi gioi thieu ngan gon.
                4. Hoi ma giam gia, voucher, coupon:
                   Vi du: "cho minh xin ma giam gia", "co voucher free ship khong", "ma nao dung duoc".
                   Goi tool tim_ma_giam_gia; noi ro dieu kien neu co.
                5. Hoi gio hang:
                   Vi du: "gio hang cua toi co gi", "kiem tra gio hang", "toi da them san pham nao".
                   Goi tool xem_gio_hang.
                6. Hoi don hang, van chuyen, thanh toan:
                   Vi du: "don cua toi den dau roi", "bao gio giao", "don nao chua thanh toan", "kiem tra trang thai don".
                   Goi tool kiem_tra_don_hang.
                7. Hoi lich su mua lai:
                   Vi du: "toi da mua gi", "mua lai san pham lan truoc", "lich su mua hang".
                   Goi tool tra_cuu_lich_su_mua_hang va goi y mua lai neu phu hop.
                8. Hoi danh muc hoac cua hang co loai nao:
                   Vi du: "shop co danh muc nao", "co nhung loai san pham nao", "toi muon xem category".
                   Goi tool xem_danh_muc.
                9. Hoi review hoac danh gia:
                   Vi du: "san pham nay review tot khong", "khach danh gia kem chong nang nay the nao".
                   Neu biet productId thi goi xem_danh_gia_san_pham; neu chua biet thi goi tra_cuu_san_pham truoc.
                10. Hoi kien thuc skincare, cach dung, thanh phan:
                    Vi du: "retinol dung sao", "BHA co hop da nhay cam khong", "routine sang toi".
                    Goi doc_bai_viet_blog neu la kien thuc chung; neu can mua san pham thi goi them tra_cuu_san_pham.
                11. Hoi xay routine:
                    Vi du: "lap routine cho da dau mun", "routine toi gian cho da kho".
                    Hoi them neu thieu loai da hoac ngan sach; neu du thong tin thi goi tra_cuu_san_pham cho tung buoc.
                12. Hoi so sanh hoac chon giua cac san pham:
                    Vi du: "nen mua A hay B", "so sanh serum vitamin C va niacinamide".
                    Goi tra_cuu_san_pham voi ten san pham/hoat chat, so sanh ngan gon theo loai da, gia, muc dich.
                13. Hoi mo ho:
                    Vi du: "tu van giup minh", "minh bi mun", "da xau qua".
                    Hoi toi da 2 cau lam ro: loai da, van de chinh, ngan sach; sau do moi de xuat san pham.
                14. Yeu cau ngoai pham vi y te:
                    Khong chan doan benh, khong hua dieu tri. Khuyen gap bac si da lieu neu co kich ung nang, viem, sung, dau, mun nang.
                """;
    }

    private ChatIntent classifyIntent(String userMessage) {
        String normalized = normalizeText(userMessage);
        if (normalized.isBlank()) {
            return ChatIntent.UNKNOWN;
        }

        if (isTopHotProductQuestion(userMessage)
                || containsAny(normalized, "best seller", "bestseller", "ban chay nhat", "dang hot")) {
            return ChatIntent.TOP_HOT_PRODUCTS;
        }
        if (containsAny(normalized, "flash sale", "flashsale", "dang sale", "giam gia", "deal", "sale hom nay")) {
            return ChatIntent.FLASH_SALE;
        }
        if (containsAny(normalized, "ma giam gia", "voucher", "coupon", "freeship", "free ship", "ma sale",
                "ma nao dung duoc", "xin ma", "lay ma")) {
            return ChatIntent.COUPON;
        }
        if (containsAny(normalized, "gio hang", "cart", "da them san pham nao", "trong gio co gi")) {
            return ChatIntent.CART;
        }
        if (containsAny(normalized, "don hang", "don cua toi", "trang thai don", "bao gio giao", "don toi dau",
                "van chuyen", "chua thanh toan", "da thanh toan")) {
            return ChatIntent.ORDER_STATUS;
        }
        if (containsAny(normalized, "lich su mua", "da mua gi", "mua lai", "lan truoc mua", "san pham da mua")) {
            return ChatIntent.PURCHASE_HISTORY;
        }
        if (containsAny(normalized, "danh muc", "category", "loai san pham", "shop co loai nao",
                "cua hang co loai nao")) {
            return ChatIntent.CATEGORY_LIST;
        }
        if (containsAny(normalized, "review", "danh gia", "feedback", "nhan xet", "khach noi gi")) {
            return ChatIntent.REVIEW_LOOKUP;
        }
        if (containsAny(normalized, "routine", "chu trinh", "sang toi", "cac buoc skincare", "lap quy trinh")) {
            return ChatIntent.SKINCARE_ROUTINE;
        }
        if (containsAny(normalized, "cach dung", "dung sao", "la gi", "thanh phan", "retinol", "bha", "aha",
                "niacinamide", "vitamin c", "ceramide", "peptide")
                && !containsAny(normalized, "san pham", "mua", "tim", "goi y", "tu van mua")) {
            return ChatIntent.BLOG_KNOWLEDGE;
        }
        if (containsAny(normalized, "san pham", "my pham", "kem", "serum", "sua rua mat", "tay trang",
                "toner", "chong nang", "duong am", "da dau", "da kho", "da mun", "da nhay cam", "tham",
                "nam", "lao hoa", "duoi", "tam gia", "ngan sach", "nen dung gi", "tu van giup")) {
            return ChatIntent.PRODUCT_SEARCH;
        }
        return ChatIntent.UNKNOWN;
    }

    private String buildDeterministicResponse(ChatIntent intent, String userMessage, String userSkinType) {
        switch (intent) {
            case TOP_HOT_PRODUCTS:
                return withQuickReplies(productService.getTopProductsSummaryForChatbot(extractRequestedLimit(userMessage, 5)),
                        "Tìm sản phẩm cho da dầu mụn|Có sản phẩm nào đang flash sale không|Cho mình xin mã giảm giá");
            case FLASH_SALE:
                return withQuickReplies(flashSaleService.getFlashSaleSummaryForChatbot(),
                        "Top sản phẩm hot nhất|Cho mình xin mã giảm giá|Tư vấn sản phẩm theo loại da");
            case COUPON:
                return withQuickReplies(couponService.getCouponsSummaryForChatbot(),
                        "Sản phẩm nào đang sale|Kiểm tra giỏ hàng của tôi|Top sản phẩm bán chạy");
            case CART:
                return withQuickReplies(getCartSummary(),
                        "Cho mình xin mã giảm giá|Đơn hàng của tôi tới đâu rồi|Tư vấn thêm sản phẩm phù hợp");
            case ORDER_STATUS:
                return withQuickReplies(orderService.getOrdersSummaryForChatbot(),
                        "Lịch sử mua hàng của tôi|Mua lại sản phẩm lần trước|Liên hệ nhân viên hỗ trợ");
            case PURCHASE_HISTORY:
                return withQuickReplies(orderService.getPurchaseHistorySummaryForChatbot(),
                        "Mua lại sản phẩm lần trước|Gợi ý sản phẩm tương tự|Cho mình xin mã giảm giá");
            case CATEGORY_LIST:
                return withQuickReplies(categoryService.getCategoriesSummaryForChatbot(),
                        "Top sản phẩm hot nhất|Tìm kem chống nắng cho da dầu|Có sản phẩm đang flash sale không");
            case REVIEW_LOOKUP:
                return withQuickReplies(buildReviewLookupResponse(userMessage),
                        "Tìm sản phẩm tương tự|Top sản phẩm được yêu thích|Tư vấn sản phẩm theo loại da");
            case SKINCARE_ROUTINE:
                return withQuickReplies(buildProductSearchResponse(buildRoutineQuery(userMessage, userSkinType)),
                        "Routine tối giản hơn|Tìm sản phẩm dưới 500k|Có sản phẩm nào đang sale không");
            case BLOG_KNOWLEDGE:
                return withQuickReplies(blogService.getBlogsSummaryForChatbot(cleanKnowledgeKeyword(userMessage)),
                        "Gợi ý sản phẩm phù hợp|Xây routine sáng tối|Tìm sản phẩm đang sale");
            case PRODUCT_SEARCH:
                if (isVagueAdviceQuestion(userMessage)) {
                    return "Mình cần thêm một chút thông tin để tư vấn đúng hơn: da bạn thuộc loại nào và vấn đề chính là gì? Ví dụ: da dầu mụn, da khô bong tróc, da nhạy cảm, thâm nám hoặc chống lão hóa. [QUICK_REPLY:Da dầu mụn nên dùng gì|Da khô nên dùng routine nào|Tìm sản phẩm dưới 500k]";
                }
                return withQuickReplies(buildProductSearchResponse(buildProductQuery(userMessage, userSkinType)),
                        "Có sản phẩm nào đang flash sale không|Top sản phẩm hot nhất|Cho mình xin mã giảm giá");
            case UNKNOWN:
            default:
                return null;
        }
    }

    private String buildProductSearchResponse(String query) {
        return productService.getProductSearchSummaryForChatbot(query);
    }

    private String buildReviewLookupResponse(String userMessage) {
        Long productId = extractProductId(userMessage);
        String productQuery = buildReviewProductQuery(userMessage);
        String productCardSummary = "";

        if (productId == null && !productQuery.isBlank()) {
            productId = productService.findFirstProductIdForChatbot(productQuery);
            productCardSummary = productService.getProductSearchSummaryForChatbot(productQuery);
        }

        if (productId == null) {
            return "Mình chưa xác định được bạn muốn xem đánh giá của sản phẩm nào. Bạn hãy gửi tên sản phẩm cụ thể hơn nhé.\n"
                    + categoryService.getCategoriesSummaryForChatbot();
        }

        String reviews = reviewService.getReviewsSummaryForChatbot(productId);
        return productCardSummary.isBlank() ? reviews : productCardSummary + "\n" + reviews;
    }

    private String buildProductQuery(String userMessage, String userSkinType) {
        StringBuilder query = new StringBuilder(stripFillerWords(userMessage));
        if (userSkinType != null && !userSkinType.isBlank() && !normalizeText(userSkinType).contains("chua ro")) {
            String normalizedMessage = normalizeText(userMessage);
            if (!containsAny(normalizedMessage, "da dau", "da kho", "da mun", "da nhay cam", "hon hop")) {
                query.append(" ").append(userSkinType);
            }
        }
        return query.toString().trim();
    }

    private String buildRoutineQuery(String userMessage, String userSkinType) {
        String base = buildProductQuery(userMessage, userSkinType);
        return (base + " sua rua mat toner serum kem duong kem chong nang").trim();
    }

    private String buildReviewProductQuery(String userMessage) {
        return stripFillerWords(userMessage)
                .replaceAll("(?i)\\b(review|danh gia|feedback|nhan xet|khach noi gi|tot khong|on khong)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanKnowledgeKeyword(String userMessage) {
        return stripFillerWords(userMessage)
                .replaceAll("(?i)\\b(la gi|dung sao|cach dung|huong dan|minh muon biet|cho minh hoi)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripFillerWords(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("(?i)\\b(cho minh|giup minh|tu van|tim|goi y|nen mua|nen dung|co san pham nao|shop co|voi|nhe|a|ạ)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Long extractProductId(String userMessage) {
        String normalized = normalizeText(userMessage);
        Matcher matcher = Pattern.compile("(?i)(?:productId|product id|san pham id|id)\\s*[:#-]?\\s*(\\d+)")
                .matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isVagueAdviceQuestion(String userMessage) {
        String normalized = normalizeText(userMessage);
        return containsAny(normalized, "tu van giup", "da xau", "nen dung gi", "goi y san pham")
                && !containsAny(normalized, "da dau", "da kho", "da mun", "da nhay cam", "tham", "nam",
                        "lao hoa", "chong nang", "duong am", "tri mun", "duoi", "tam gia");
    }

    private String withQuickReplies(String response, String quickReplies) {
        if (response == null || response.isBlank()) {
            return "Mình chưa có dữ liệu phù hợp để trả lời chính xác. Bạn hãy nói rõ hơn nhu cầu, loại da hoặc ngân sách nhé. [QUICK_REPLY:Tư vấn theo loại da|Top sản phẩm hot nhất|Có mã giảm giá nào không]";
        }
        if (response.contains("[QUICK_REPLY:")) {
            return response;
        }
        return response + "\n[QUICK_REPLY:" + quickReplies + "]";
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        String unsigned = Product.removeVietnameseAccents(lower);
        return (unsigned == null ? lower : unsigned)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isTopHotProductQuestion(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String normalized = Product.removeVietnameseAccents(userMessage.toLowerCase());
        if (normalized == null) {
            normalized = userMessage.toLowerCase();
        }

        boolean asksProduct = normalized.contains("san pham") || normalized.contains("my pham")
                || normalized.contains("hang") || normalized.contains("mon");
        boolean asksRanking = normalized.contains("top") || normalized.contains("hot")
                || normalized.contains("ban chay") || normalized.contains("pho bien")
                || normalized.contains("yeu thich") || normalized.contains("noi bat")
                || normalized.contains("best seller") || normalized.contains("bestseller");

        return asksProduct && asksRanking;
    }

    private int extractRequestedLimit(String userMessage, int fallback) {
        if (userMessage == null) {
            return fallback;
        }
        Matcher matcher = Pattern.compile("\\b(\\d{1,2})\\b").matcher(userMessage);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            int limit = Integer.parseInt(matcher.group(1));
            return Math.max(1, Math.min(limit, 10));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String updateSkinType(String skinType) {
        try {
            String email = SecurityUtil.getCurrentUserLogin().orElse(null);
            if (email == null)
                return "Lỗi: Khách chưa đăng nhập nên không lưu được.";
            User user = userRepository.findByEmail(email);
            if (user != null && user.getUserProfile() != null) {
                user.getUserProfile().setSkinType(skinType);
                userProfileRepository.save(user.getUserProfile());

                // Real-time notification to the user
                notificationService.createNotification(
                        user,
                        "Cập nhật loại da",
                        "AI đã xác định và cập nhật loại da của bạn là: " + skinType,
                        "AI_INSIGHT");

                // Refresh message via WebSocket
                messagingTemplate.convertAndSendToUser(
                        email.toLowerCase(),
                        "/queue/skin-type-updates",
                        Map.of("skinType", skinType));

                return "Thành công: Đã cập nhật loại da là " + skinType + " vào hồ sơ của "
                        + user.getUserProfile().getName();
            }
            return "Lỗi: Không tìm thấy hồ sơ.";
        } catch (Exception e) {
            return "Lỗi hệ thống khi cập nhật loại da.";
        }
    }

    private void saveChatInteraction(String email, String msg, String resp, String sid, String device, String url,
            String intent) {
        try {
            ChatInteraction interaction = new ChatInteraction();
            interaction.setUserEmail(email);
            interaction.setUserMessage(msg);
            interaction.setAiResponse(resp);
            interaction.setSessionId(sid);
            interaction.setDeviceType(device);
            interaction.setPageUrl(url);
            interaction.setIntent(intent);
            chatInteractionRepository.save(interaction);
        } catch (Exception e) {
        }
    }

    private String getCartSummary() {
        try {
            Cart cart = cartService.getOrCreateCart();
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
                return "Giỏ hàng trống.";
            StringBuilder sb = new StringBuilder("Giỏ hàng:\n");
            for (CartItem item : cart.getItems())
                sb.append("- ").append(item.getProduct().getName()).append(" (SL: ").append(item.getQuantity())
                        .append(")\n");
            return sb.toString();
        } catch (Exception e) {
            return "Lỗi giỏ hàng.";
        }
    }

    private void logChatActivity(String email, String userMsg, String aiResp, String sid, String device, String url) {
        try {
            Map<String, Object> logMeta = new HashMap<>();
            logMeta.put("userMessage", userMsg);
            logMeta.put("responsePreview", aiResp.length() > 50 ? aiResp.substring(0, 50) + "..." : aiResp);
            trackingService.logActivity(email != null ? email : "anonymous", "server", "CHAT_WITH_BOT",
                    objectMapper.writeValueAsString(logMeta), sid, device, null, url);
        } catch (Exception e) {
        }
    }
}
