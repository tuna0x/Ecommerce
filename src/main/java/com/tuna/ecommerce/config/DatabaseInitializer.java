package com.tuna.ecommerce.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Permission;
import com.tuna.ecommerce.domain.Promotion;
import com.tuna.ecommerce.domain.Role;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserProfile;
import com.tuna.ecommerce.repository.BannerRepository;
import com.tuna.ecommerce.repository.BrandRepository;
import com.tuna.ecommerce.repository.CouponRepository;
import com.tuna.ecommerce.repository.PermissionRepository;
import com.tuna.ecommerce.repository.PromotionRepository;
import com.tuna.ecommerce.repository.RoleRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.constant.GenderEnum;
import com.tuna.ecommerce.ultil.constant.PromotionTypeEnum;
 
 import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrandRepository brandRepository;
    private final BannerRepository bannerRepository;
    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.tuna.ecommerce.repository.ProductRepository productRepository;
    private final com.tuna.ecommerce.repository.BlogRepository blogRepository;

    public DatabaseInitializer(PermissionRepository permissionRepository, RoleRepository roleRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder, BrandRepository brandRepository,
            BannerRepository bannerRepository, PromotionRepository promotionRepository,
            CouponRepository couponRepository, org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            com.tuna.ecommerce.repository.ProductRepository productRepository,
            com.tuna.ecommerce.repository.BlogRepository blogRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.brandRepository = brandRepository;
        this.bannerRepository = bannerRepository;
        this.promotionRepository = promotionRepository;
        this.couponRepository = couponRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.blogRepository = blogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("\u003e\u003e\u003e START INIT DATABASE");

        // Schema Cleanup (Safe/Idempotent)

        try {
            jdbcTemplate.execute("ALTER TABLE products DROP COLUMN stock");
            jdbcTemplate.execute("ALTER TABLE product_variants DROP COLUMN stock");
            jdbcTemplate.execute("ALTER TABLE product_variants DROP COLUMN active");
        } catch (Exception e) {
        }

        // 1. Ensure Roles exist
        Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("SUPER_ADMIN");
            adminRole.setDescription("Full control role");
            adminRole.setActive(true);
            adminRole = this.roleRepository.save(adminRole);
        }

        Role userRole = this.roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole.setDescription("Regular customer role");
            userRole.setActive(true);
            userRole = this.roleRepository.save(userRole);
        }

        // 2. Sync All Permissions
        syncAllPermissions(adminRole, userRole);

        // 3. Ensure Users exist
        long countUsers = this.userRepository.count();
        if (countUsers == 0) {
            // Create Admin
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(this.passwordEncoder.encode("123456"));
            UserProfile adminProfile = new UserProfile();
            adminProfile.setName("SUPER ADMIN");
            adminProfile.setAge(20);
            adminProfile.setGender(GenderEnum.MALE);
            adminProfile.setImage("https://ui-avatars.com/api/?name=Super+Admin&background=random");
            adminProfile.setUser(admin);
            admin.setUserProfile(adminProfile);
            admin.setRole(adminRole);
            this.userRepository.save(admin);

            // Create Regular User
            User normalUser = new User();
            normalUser.setEmail("user@gmail.com");
            normalUser.setPassword(this.passwordEncoder.encode("123456"));
            UserProfile userProfile = new UserProfile();
            userProfile.setName("REGULAR USER");
            userProfile.setAge(18);
            userProfile.setGender(GenderEnum.FEMALE);
            userProfile.setImage("https://ui-avatars.com/api/?name=Regular+User&background=random");
            userProfile.setUser(normalUser);
            normalUser.setUserProfile(userProfile);
            normalUser.setRole(userRole);
            this.userRepository.save(normalUser);
            log.info("\u003e\u003e\u003e CREATED DEFAULT USERS");
        }

        // 4. Create a Global Promotion if none exists
        if (this.promotionRepository.count() == 0) {
            Promotion globalPromo = new Promotion();
            globalPromo.setName("Mừng khai trương");
            globalPromo.setDescription("Giảm giá 10% toàn bộ cửa hàng");
            globalPromo.setType(PromotionTypeEnum.PERCENT);
            globalPromo.setDiscountValue(BigDecimal.valueOf(10));
            globalPromo.setActive(true);
            globalPromo.setGlobal(true);
            globalPromo.setStartAt(LocalDateTime.now().minusDays(1));
            globalPromo.setEndAt(LocalDateTime.now().plusDays(30));
            this.promotionRepository.save(globalPromo);
            log.info("\u003e\u003e\u003e CREATED DEFAULT GLOBAL PROMOTION");
        }

        // 5. Create Default Coupons if none exist
        if (this.couponRepository.count() == 0) {
            com.tuna.ecommerce.domain.Coupon c1 = new com.tuna.ecommerce.domain.Coupon();
            c1.setName("Chào mừng bạn mới");
            c1.setDescription("Giảm 50k cho đơn hàng đầu tiên");
            c1.setCode("WELCOME50");
            c1.setType(com.tuna.ecommerce.ultil.constant.CouponTypeEnum.FIXED);
            c1.setDiscountValue(BigDecimal.valueOf(50000));
            c1.setMinOrderValue(BigDecimal.valueOf(200000));
            c1.setStartDate(LocalDateTime.now().minusDays(1));
            c1.setEndDate(LocalDateTime.now().plusDays(90));
            c1.setStatus(com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE);
            c1.setPublic(true);
            c1.setUsageLimit(1000);
            c1.setUsedCount(0);
            this.couponRepository.save(c1);

            com.tuna.ecommerce.domain.Coupon c2 = new com.tuna.ecommerce.domain.Coupon();
            c2.setName("Siêu sale mùa hè");
            c2.setDescription("Giảm 15% tối đa 100k");
            c2.setCode("SUMMER15");
            c2.setType(com.tuna.ecommerce.ultil.constant.CouponTypeEnum.PERCENT);
            c2.setDiscountValue(BigDecimal.valueOf(15));
            c2.setMaxDiscountValue(BigDecimal.valueOf(100000));
            c2.setMinOrderValue(BigDecimal.valueOf(300000));
            c2.setStartDate(LocalDateTime.now().minusDays(1));
            c2.setEndDate(LocalDateTime.now().plusDays(30));
            c2.setStatus(com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE);
            c2.setPublic(true);
            c2.setUsageLimit(500);
            c2.setUsedCount(0);
            this.couponRepository.save(c2);

            com.tuna.ecommerce.domain.Coupon c3 = new com.tuna.ecommerce.domain.Coupon();
            c3.setName("Chào mừng bạn mới");
            c3.setDescription("Giảm 20% cho đơn hàng đầu tiên");
            c3.setCode("BONG20");
            c3.setType(com.tuna.ecommerce.ultil.constant.CouponTypeEnum.PERCENT);
            c3.setDiscountValue(BigDecimal.valueOf(20));
            c3.setMinOrderValue(BigDecimal.valueOf(100000));
            c3.setStartDate(LocalDateTime.now().minusDays(1));
            c3.setEndDate(LocalDateTime.now().plusDays(365));
            c3.setStatus(com.tuna.ecommerce.ultil.constant.CouponStatus.ACTIVE);
            c3.setPublic(false);
            c3.setFirstOrderOnly(true);
            c3.setUsageLimit(10000);
            c3.setUsedCount(0);
            this.couponRepository.save(c3);
            log.info("\u003e\u003e\u003e CREATED DEFAULT COUPONS");
        }

        // 5.1 Cleanup existing Coupons with null usedCount
        try {
            jdbcTemplate.execute("UPDATE coupons SET used_count = 0 WHERE used_count IS NULL");
            jdbcTemplate.execute("UPDATE coupons SET usage_limit = 1000 WHERE usage_limit IS NULL");
        } catch (Exception e) {
            log.error("\u003e\u003e\u003e FAILED TO CLEANUP COUPONS: {}", e.getMessage());
        }

        // 6. Sync Product nameUnsigned
        List<com.tuna.ecommerce.domain.Product> productsForSync = this.productRepository.findAll();
        for (com.tuna.ecommerce.domain.Product p : productsForSync) {
            if (p.getNameUnsigned() == null) {
                p.setNameUnsigned(com.tuna.ecommerce.domain.Product.removeVietnameseAccents(p.getName()));
                this.productRepository.save(p);
            }
        }
        log.info("\u003e\u003e\u003e SYNCED NAME_UNSIGNED FOR PRODUCTS");

        // 7. Seed Blogs
        if (this.blogRepository.count() == 0) {
            com.tuna.ecommerce.domain.Blog b1 = new com.tuna.ecommerce.domain.Blog();
            b1.setTitle("10 Bước Skincare Hàn Quốc Cho Làn Da Hoàn Hảo");
            b1.setExcerpt("Khám phá quy trình chăm sóc da 10 bước nổi tiếng của Hàn Quốc giúp bạn có làn da căng bóng, mịn màng như idol K-pop.");
            b1.setContent("<h2>Quy trình skincare 10 bước chuẩn Hàn</h2><p>Làn da thủy tinh (glass skin) không tự nhiên mà có. Đó là kết quả của một quy trình chăm sóc tỉ mỉ...</p>");
            b1.setImage("https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=600&h=400&fit=crop");
            b1.setCategory("Chăm sóc da");
            b1.setAuthor("Thu Trang");
            b1.setReadTime("8 phút");
            this.blogRepository.save(b1);

            com.tuna.ecommerce.domain.Blog b2 = new com.tuna.ecommerce.domain.Blog();
            b2.setTitle("Review Serum Vitamin C: Top 5 Sản Phẩm Đáng Mua Nhất 2024");
            b2.setExcerpt("So sánh chi tiết 5 loại serum Vitamin C bán chạy nhất hiện nay từ thành phần, hiệu quả đến giá thành.");
            b2.setContent("<h2>Tại sao Serum Vitamin C lại quan trọng?</h2><p>Vitamin C là thành phần chống oxy hóa mạnh mẽ giúp làm sáng da và mờ thâm...</p>");
            b2.setImage("https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=600&h=400&fit=crop");
            b2.setCategory("Review sản phẩm");
            b2.setAuthor("Minh Đức");
            b2.setReadTime("10 phút");
            this.blogRepository.save(b2);

            log.info("\u003e\u003e\u003e CREATED DEFAULT BLOGS");
        }

        log.info("\u003e\u003e\u003e FINISH INIT DATABASE");
    }

    private void syncAllPermissions(Role adminRole, Role userRole) {
        List<PermDef> perms = new ArrayList<>();

        // PERMISSIONS
        perms.add(new PermDef("Create a permission", "/api/v1/permissions", "POST", "PERMISSIONS", false));
        perms.add(new PermDef("Update a permission", "/api/v1/permissions", "PUT", "PERMISSIONS", false));
        perms.add(new PermDef("Delete a permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS", false));
        perms.add(new PermDef("Get a permission by id", "/api/v1/permissions/{id}", "GET", "PERMISSIONS", false));
        perms.add(new PermDef("Get permissions with pagination", "/api/v1/permissions", "GET", "PERMISSIONS", false));

        // ROLES
        perms.add(new PermDef("Create a role", "/api/v1/roles", "POST", "ROLES", false));
        perms.add(new PermDef("Update a role", "/api/v1/roles", "PUT", "ROLES", false));
        perms.add(new PermDef("Delete a role", "/api/v1/roles/{id}", "DELETE", "ROLES", false));
        perms.add(new PermDef("Get a role by id", "/api/v1/roles/{id}", "GET", "ROLES", false));
        perms.add(new PermDef("Get roles with pagination", "/api/v1/roles", "GET", "ROLES", false));

        // USERS
        perms.add(new PermDef("Create a user", "/api/v1/users", "POST", "USERS", false));
        perms.add(new PermDef("Update a user", "/api/v1/users", "PUT", "USERS", true));
        perms.add(new PermDef("Delete a user", "/api/v1/users/{id}", "DELETE", "USERS", false));
        perms.add(new PermDef("Get a user by id", "/api/v1/users/{id}", "GET", "USERS", false));
        perms.add(new PermDef("Get users with pagination", "/api/v1/users", "GET", "USERS", false));
        perms.add(new PermDef("Toggle user active status", "/api/v1/users/{id}/active", "PATCH", "USERS", false));
        perms.add(new PermDef("Update user role", "/api/v1/users/{id}/role", "PATCH", "USERS", false));
        perms.add(new PermDef("Get user analytics 360", "/api/v1/users/{id}/analytics", "GET", "USERS", false));
        perms.add(
                new PermDef("Update admin notes for user", "/api/v1/users/{id}/admin-notes", "PATCH", "USERS", false));
        perms.add(new PermDef("Check email existence", "/api/v1/auth/check-email", "GET", "USERS", true));
        perms.add(new PermDef("Change password", "/api/v1/auth/change-password", "POST", "AUTH", true));

        // DASHBOARD
        perms.add(new PermDef("Get dashboard statistics", "/api/v1/dashboard/statistics", "GET", "DASHBOARD", false));
        perms.add(new PermDef("Export dashboard statistics to Excel", "/api/v1/dashboard/export-excel", "GET",
                "DASHBOARD", false));

        // PRODUCTS
        perms.add(new PermDef("Create a product", "/api/v1/products", "POST", "PRODUCTS", false));
        perms.add(new PermDef("Update a product", "/api/v1/products", "PUT", "PRODUCTS", false));
        perms.add(new PermDef("Delete a product", "/api/v1/products/{id}", "DELETE", "PRODUCTS", false));
        perms.add(new PermDef("Get a product by id", "/api/v1/products/{id}", "GET", "PRODUCTS", true));
        perms.add(new PermDef("Get products with pagination", "/api/v1/products", "GET", "PRODUCTS", true));
        perms.add(new PermDef("Get related products", "/api/v1/products/{id}/related", "GET", "PRODUCTS", true));
        perms.add(new PermDef("Get flash sale products", "/api/v1/products/flash-sale", "GET", "PRODUCTS", true));

        // CATEGORIES
        perms.add(new PermDef("Create a category", "/api/v1/categories", "POST", "CATEGORIES", false));
        perms.add(new PermDef("Update a category", "/api/v1/categories", "PUT", "CATEGORIES", false));
        perms.add(new PermDef("Delete a category", "/api/v1/categories/{id}", "DELETE", "CATEGORIES", false));
        perms.add(new PermDef("Get a category by id", "/api/v1/categories/{id}", "GET", "CATEGORIES", true));
        perms.add(new PermDef("Get categories with pagination", "/api/v1/categories", "GET", "CATEGORIES", true));

        // BRANDS
        perms.add(new PermDef("Create a brand", "/api/v1/brands", "POST", "BRANDS", false));
        perms.add(new PermDef("Update a brand", "/api/v1/brands", "PUT", "BRANDS", false));
        perms.add(new PermDef("Delete a brand", "/api/v1/brands/{id}", "DELETE", "BRANDS", false));
        perms.add(new PermDef("Get a brand by id", "/api/v1/brands/{id}", "GET", "BRANDS", true));
        perms.add(new PermDef("Get brands with pagination", "/api/v1/brands", "GET", "BRANDS", true));

        // ATTRIBUTES
        perms.add(new PermDef("Create a attribute", "/api/v1/attributes", "POST", "ATTRIBUTES", false));
        perms.add(new PermDef("Update a attribute", "/api/v1/attributes", "PUT", "ATTRIBUTES", false));
        perms.add(new PermDef("Delete a attribute", "/api/v1/attributes/{id}", "DELETE", "ATTRIBUTES", false));
        perms.add(new PermDef("Get a attribute by id", "/api/v1/attributes/{id}", "GET", "ATTRIBUTES", true));
        perms.add(new PermDef("Get attributes with pagination", "/api/v1/attributes", "GET", "ATTRIBUTES", true));

        // BANNERS
        perms.add(new PermDef("Create a banner", "/api/v1/banners", "POST", "BANNERS", false));
        perms.add(new PermDef("Update a banner", "/api/v1/banners", "PUT", "BANNERS", false));
        perms.add(new PermDef("Delete a banner", "/api/v1/banners/{id}", "DELETE", "BANNERS", false));
        perms.add(new PermDef("Get a banner by id", "/api/v1/banners/{id}", "GET", "BANNERS", true));
        perms.add(new PermDef("Get banners with pagination", "/api/v1/banners", "GET", "BANNERS", true));
        perms.add(new PermDef("Toggle banner active status", "/api/v1/banners/{id}/active", "PATCH", "BANNERS", false));

        // ATTRIBUTE VALUES
        perms.add(
                new PermDef("Create a attribute value", "/api/v1/attributes-values", "POST", "ATTRIBUTE VALUE", false));
        perms.add(
                new PermDef("Update a attribute value", "/api/v1/attributes-values", "PUT", "ATTRIBUTE VALUE", false));
        perms.add(new PermDef("Delete a attribute value", "/api/v1/attributes-values/{id}", "DELETE", "ATTRIBUTE VALUE",
                false));
        perms.add(new PermDef("Get a attribute value by id", "/api/v1/attributes-values/{id}", "GET", "ATTRIBUTE VALUE",
                true));
        perms.add(new PermDef("Get attribute values with pagination", "/api/v1/attributes-values", "GET",
                "ATTRIBUTE VALUE", true));

        // PRODUCT DETAIL
        perms.add(new PermDef("Create a product detail", "/api/v1/product-detail", "POST", "PRODUCT DETAIL", false));
        perms.add(new PermDef("Update a product detail", "/api/v1/product-detail", "PUT", "PRODUCT DETAIL", false));
        perms.add(new PermDef("Delete a product detail", "/api/v1/product-detail/{id}", "DELETE", "PRODUCT DETAIL",
                false));
        perms.add(new PermDef("Get a product detail by id", "/api/v1/product-detail/{id}", "GET", "PRODUCT DETAIL",
                true));
        perms.add(new PermDef("Get product-detail with pagination", "/api/v1/product-detail", "GET", "PRODUCT DETAIL",
                true));

        // COUPONS
        perms.add(new PermDef("Create a coupon", "/api/v1/coupons", "POST", "COUPONS", false));
        perms.add(new PermDef("Update a coupon", "/api/v1/coupons", "PUT", "COUPONS", false));
        perms.add(new PermDef("Delete a coupon", "/api/v1/coupons/{id}", "DELETE", "COUPONS", false));
        perms.add(new PermDef("Get a coupon by id", "/api/v1/coupons/{id}", "GET", "COUPONS", false));
        perms.add(new PermDef("Get coupons with pagination", "/api/v1/coupons", "GET", "COUPONS", false));
        perms.add(new PermDef("Toggle coupon active status", "/api/v1/coupons/{id}/active", "PATCH", "COUPONS", false));
        perms.add(new PermDef("Validate a coupon code", "/api/v1/coupons/validate", "GET", "COUPONS", true));

        // USER COUPONS (WALLET)
        perms.add(new PermDef("Collect a coupon", "/api/v1/user-coupons/collect/{id}", "POST", "USER COUPONS", true));
        perms.add(new PermDef("Get my coupons", "/api/v1/user-coupons/my", "GET", "USER COUPONS", true));
        perms.add(new PermDef("Get available coupons", "/api/v1/user-coupons/available", "GET", "USER COUPONS", true));

        // PROMOTIONS
        perms.add(new PermDef("Create a promotion", "/api/v1/promotions", "POST", "PROMOTIONS", false));
        perms.add(new PermDef("Update a promotion", "/api/v1/promotions", "PUT", "PROMOTIONS", false));
        perms.add(new PermDef("Delete a promotion", "/api/v1/promotions/{id}", "DELETE", "PROMOTIONS", false));
        perms.add(new PermDef("Get a promotion by id", "/api/v1/promotions/{id}", "GET", "PROMOTIONS", false));
        perms.add(new PermDef("Get promotions with pagination", "/api/v1/promotions", "GET", "PROMOTIONS", false));
        perms.add(new PermDef("Active a promotion", "/api/v1/promotions/active/{id}", "POST", "PROMOTIONS", false));
        perms.add(new PermDef("Deactive a promotion", "/api/v1/promotions/deactive/{id}", "POST", "PROMOTIONS", false));
        perms.add(new PermDef("Toggle promotion active status", "/api/v1/promotions/{id}/active", "PATCH", "PROMOTIONS",
                false));
        perms.add(new PermDef("Assign promotion to product", "/api/v1/product-promotions", "POST", "PRODUCT PROMOTIONS",
                false));
        perms.add(new PermDef("Get assigned products", "/api/v1/promotions/{id}/products", "GET", "PROMOTIONS", false));
        perms.add(new PermDef("Assign products to promotion", "/api/v1/promotions/{id}/products", "POST", "PROMOTIONS",
                false));
        perms.add(new PermDef("Assign all products to promotion", "/api/v1/promotions/{id}/products/all", "POST",
                "PROMOTIONS", false));

        // ADDRESSES
        perms.add(new PermDef("Create a address", "/api/v1/addresses", "POST", "ADDRESSES", true));
        perms.add(new PermDef("Update a address", "/api/v1/addresses", "PUT", "ADDRESSES", true));
        perms.add(new PermDef("Delete a address", "/api/v1/addresses/{id}", "DELETE", "ADDRESSES", true));
        perms.add(new PermDef("Set address default", "/api/v1/addresses/{id}/default", "PUT", "ADDRESSES", true));
        perms.add(new PermDef("Get addresses with pagination", "/api/v1/addresses", "GET", "ADDRESSES", true));

        // CART
        perms.add(new PermDef("Add to cart", "/api/v1/cart", "POST", "CART", true));
        perms.add(new PermDef("Get a cart", "/api/v1/cart", "GET", "CART", true));
        perms.add(new PermDef("Update item in cart", "/api/v1/cart", "PUT", "CART", true));
        perms.add(new PermDef("Delete item in cart by id", "/api/v1/cart/{id}", "DELETE", "CART", true));

        // WISHLIST
        perms.add(new PermDef("VIEW wishlist", "/api/v1/wishlist", "GET", "WISHLIST", true));
        perms.add(new PermDef("VIEW wishlist check", "/api/v1/wishlist/check/{productId}", "GET", "WISHLIST", true));
        perms.add(new PermDef("CREATE wishlist", "/api/v1/wishlist/{productId}", "POST", "WISHLIST", true));
        perms.add(new PermDef("DELETE wishlist", "/api/v1/wishlist/{productId}", "DELETE", "WISHLIST", true));

        // ORDER
        perms.add(new PermDef("Create a order", "/api/v1/order/checkout", "POST", "ORDER", true));
        perms.add(new PermDef("Get order by id", "/api/v1/order/{id}", "GET", "ORDER", true));
        perms.add(new PermDef("Get my orders with pagination", "/api/v1/order/me", "GET", "ORDER", true));
        perms.add(new PermDef("Admin get all orders", "/api/v1/order/admin/all", "GET", "ORDER", false));
        perms.add(new PermDef("Update order status", "/api/v1/order/{id}/status", "PUT", "ORDER", false));
        perms.add(new PermDef("Update order address", "/api/v1/order/{id}/address", "PUT", "ORDER", false));
        perms.add(new PermDef("Bulk update order status", "/api/v1/order/bulk-status", "POST", "ORDER", false));
        perms.add(new PermDef("Bulk create ghn shipping orders", "/api/v1/order/bulk-ghn", "POST", "ORDER", false));
        perms.add(new PermDef("Cancel order", "/api/v1/order/{id}/cancel", "PUT", "ORDER", true));

        // PAYMENT
        perms.add(new PermDef("Confirm payment", "/api/v1/payment/confirm", "POST", "PAYMENT", true));

        // TRANSACTIONS
        perms.add(new PermDef("Get all transactions with pagination", "/api/v1/transactions", "GET", "TRANSACTIONS",
                false));

        // PRICING
        perms.add(new PermDef("Get Product price", "/api/v1/price/{id}", "GET", "PRICING PRODUCT", true));

        // REVIEWS
        perms.add(new PermDef("Create a review", "/api/v1/reviews", "POST", "REVIEWS", true));
        perms.add(new PermDef("Get reviews by product", "/api/v1/reviews/product/{productId}", "GET", "REVIEWS", true));
        perms.add(new PermDef("Get featured reviews", "/api/v1/reviews/featured", "GET", "REVIEWS", true));
        perms.add(new PermDef("Delete a review", "/api/v1/reviews/{id}", "DELETE", "REVIEWS", true));

        // CHAT
        perms.add(new PermDef("Send chat message", "/api/v1/chat", "POST", "CHAT", true));
        perms.add(new PermDef("Get chat history", "/api/v1/chat/history", "GET", "CHAT", true));
        perms.add(new PermDef("Get recent conversations", "/api/v1/chat/conversations", "GET", "CHAT", true));

        // INVENTORY
        perms.add(new PermDef("Get inventory with pagination", "/api/v1/inventory", "GET", "INVENTORY", false));
        perms.add(new PermDef("Manual stock adjustment", "/api/v1/inventory/adjust", "POST", "INVENTORY", false));
        perms.add(new PermDef("Bulk stock adjustment", "/api/v1/inventory/bulk-adjust", "POST", "INVENTORY", false));
        perms.add(new PermDef("Get inventory logs", "/api/v1/inventory/{id}/logs", "GET", "INVENTORY", false));
        perms.add(new PermDef("Get all inventory logs", "/api/v1/inventory/logs", "GET", "INVENTORY", false));
        perms.add(new PermDef("Export inventory logs to Excel", "/api/v1/inventory/logs/export", "GET", "INVENTORY",
                false));

        // NOTIFICATIONS
        perms.add(new PermDef("Get notifications for current user", "/api/v1/notifications", "GET", "NOTIFICATIONS",
                true));
        perms.add(new PermDef("Mark notification as read", "/api/v1/notifications/{id}/read", "PUT", "NOTIFICATIONS",
                true));
        perms.add(new PermDef("Mark all notifications as read", "/api/v1/notifications/read-all", "PUT",
                "NOTIFICATIONS", true));
        perms.add(new PermDef("Count unread notifications", "/api/v1/notifications/unread-count", "GET",
                "NOTIFICATIONS", true));
        perms.add(
                new PermDef("Admin sends notification", "/api/v1/notifications/send", "POST", "NOTIFICATIONS", false));

        // TRACKING
        perms.add(new PermDef("Get all tracking logs", "/api/v1/tracking/logs", "GET", "TRACKING", false));
        perms.add(new PermDef("Get tracking analytics", "/api/v1/tracking/analytics", "GET", "TRACKING", false));

        // BLOGS
        perms.add(new PermDef("Create a blog", "/api/v1/blogs", "POST", "BLOGS", false));
        perms.add(new PermDef("Update a blog", "/api/v1/blogs", "PUT", "BLOGS", false));
        perms.add(new PermDef("Delete a blog", "/api/v1/blogs/{id}", "DELETE", "BLOGS", false));
        perms.add(new PermDef("Get a blog by id", "/api/v1/blogs/{id}", "GET", "BLOGS", true));
        perms.add(new PermDef("Get blogs with pagination", "/api/v1/blogs", "GET", "BLOGS", true));

        // SUBSCRIBERS
        perms.add(new PermDef("Subscribe to newsletter", "/api/v1/subscribers", "POST", "SUBSCRIBERS", true));
        perms.add(new PermDef("Get subscribers with pagination", "/api/v1/subscribers", "GET", "SUBSCRIBERS", false));
        perms.add(new PermDef("Delete a subscriber", "/api/v1/subscribers/{id}", "DELETE", "SUBSCRIBERS", false));
 
        // FLASH SALE
        perms.add(new PermDef("Create a flash sale", "/api/v1/flash-sales", "POST", "FLASH SALE", false));
        perms.add(new PermDef("Update a flash sale", "/api/v1/flash-sales/{id}", "PUT", "FLASH SALE", false));
        perms.add(new PermDef("Delete a flash sale", "/api/v1/flash-sales/{id}", "DELETE", "FLASH SALE", false));
        perms.add(new PermDef("Get flash sales", "/api/v1/flash-sales", "GET", "FLASH SALE", true));
        perms.add(new PermDef("Get active flash sale", "/api/v1/flash-sales/active", "GET", "FLASH SALE", true));

        // CONTACT
        perms.add(new PermDef("Send contact message", "/api/v1/public/contact", "POST", "CONTACT", true));
        perms.add(new PermDef("Get contact messages with pagination", "/api/v1/contact", "GET", "CONTACT", false));
        perms.add(new PermDef("Delete a contact message", "/api/v1/contact/{id}", "DELETE", "CONTACT", false));
        perms.add(new PermDef("Update contact message status", "/api/v1/contact/{id}/status", "PATCH", "CONTACT", false));

        boolean updated = false;
        for (PermDef def : perms) {
            Permission p = this.permissionRepository.findByModuleAndApiPathAndMethod(def.module, def.path, def.method);
            if (p == null) {
                p = new Permission(def.name, def.path, def.method, def.module);
                p = this.permissionRepository.save(p);
            } else if (!p.getName().equals(def.name)) {
                p.setName(def.name);
                p = this.permissionRepository.save(p);
            }

            final Permission finalP = p;
            if (adminRole != null) {
                if (adminRole.getPermissions() == null) {
                    adminRole.setPermissions(new ArrayList<>());
                }
                if (adminRole.getPermissions().stream().noneMatch(x -> x != null && x.getId().equals(finalP.getId()))) {
                    adminRole.getPermissions().add(p);
                    updated = true;
                }
            }

            if (def.toUser && userRole != null) {
                if (userRole.getPermissions() == null) {
                    userRole.setPermissions(new ArrayList<>());
                }
                if (userRole.getPermissions().stream().noneMatch(x -> x != null && x.getId().equals(finalP.getId()))) {
                    userRole.getPermissions().add(p);
                    updated = true;
                }
            }
        }

        if (updated) {
            if (adminRole != null)
                this.roleRepository.save(adminRole);
            if (userRole != null)
                this.roleRepository.save(userRole);
        }
    }

    private static class PermDef {
        String name;
        String path;
        String method;
        String module;
        boolean toUser;

        PermDef(String name, String path, String method, String module, boolean toUser) {
            this.name = name;
            this.path = path;
            this.method = method;
            this.module = module;
            this.toUser = toUser;
        }
    }
}
