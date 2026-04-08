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

@Service
public class DatabaseInitializer implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrandRepository brandRepository;
    private final BannerRepository bannerRepository;
    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;

    public DatabaseInitializer(PermissionRepository permissionRepository, RoleRepository roleRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder, BrandRepository brandRepository,
            BannerRepository bannerRepository, PromotionRepository promotionRepository,
            CouponRepository couponRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.brandRepository = brandRepository;
        this.bannerRepository = bannerRepository;
        this.promotionRepository = promotionRepository;
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> START INIT DATABASE");

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
            System.out.println(">>> CREATED DEFAULT USERS");
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
            System.out.println(">>> CREATED DEFAULT GLOBAL PROMOTION");
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
            this.couponRepository.save(c2);

            System.out.println(">>> CREATED DEFAULT COUPONS");
        }

        System.out.println(">>> FINISH INIT DATABASE");
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
        perms.add(new PermDef("Update a user", "/api/v1/users", "PUT", "USERS", false));
        perms.add(new PermDef("Delete a user", "/api/v1/users/{id}", "DELETE", "USERS", false));
        perms.add(new PermDef("Get a user by id", "/api/v1/users/{id}", "GET", "USERS", false));
        perms.add(new PermDef("Get users with pagination", "/api/v1/users", "GET", "USERS", false));
        perms.add(new PermDef("Toggle user active status", "/api/v1/users/{id}/active", "PATCH", "USERS", false));
        perms.add(new PermDef("Update user role", "/api/v1/users/{id}/role", "PATCH", "USERS", false));

        // DASHBOARD
        perms.add(new PermDef("Get dashboard statistics", "/api/v1/dashboard/statistics", "GET", "DASHBOARD", false));

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
        perms.add(new PermDef("Create a attribute value", "/api/v1/attributes-values", "POST", "ATTRIBUTE VALUE", false));
        perms.add(new PermDef("Update a attribute value", "/api/v1/attributes-values", "PUT", "ATTRIBUTE VALUE", false));
        perms.add(new PermDef("Delete a attribute value", "/api/v1/attributes-values/{id}", "DELETE", "ATTRIBUTE VALUE", false));
        perms.add(new PermDef("Get a attribute value by id", "/api/v1/attributes-values/{id}", "GET", "ATTRIBUTE VALUE", true));
        perms.add(new PermDef("Get attribute values with pagination", "/api/v1/attributes-values", "GET", "ATTRIBUTE VALUE", true));

        // PRODUCT DETAIL
        perms.add(new PermDef("Create a product detail", "/api/v1/product-detail", "POST", "PRODUCT DETAIL", false));
        perms.add(new PermDef("Update a product detail", "/api/v1/product-detail", "PUT", "PRODUCT DETAIL", false));
        perms.add(new PermDef("Delete a product detail", "/api/v1/product-detail/{id}", "DELETE", "PRODUCT DETAIL", false));
        perms.add(new PermDef("Get a product detail by id", "/api/v1/product-detail/{id}", "GET", "PRODUCT DETAIL", true));
        perms.add(new PermDef("Get product-detail with pagination", "/api/v1/product-detail", "GET", "PRODUCT DETAIL", true));

        // COUPONS
        perms.add(new PermDef("Create a coupon", "/api/v1/coupons", "POST", "COUPONS", false));
        perms.add(new PermDef("Update a coupon", "/api/v1/coupons", "PUT", "COUPONS", false));
        perms.add(new PermDef("Delete a coupon", "/api/v1/coupons/{id}", "DELETE", "COUPONS", false));
        perms.add(new PermDef("Get a coupon by id", "/api/v1/coupons/{id}", "GET", "COUPONS", false));
        perms.add(new PermDef("Get coupons with pagination", "/api/v1/coupons", "GET", "COUPONS", false));
        perms.add(new PermDef("Toggle coupon active status", "/api/v1/coupons/{id}/active", "PATCH", "COUPONS", false));

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
        perms.add(new PermDef("Toggle promotion active status", "/api/v1/promotions/{id}/active", "PATCH", "PROMOTIONS", false));
        perms.add(new PermDef("Assign promotion to product", "/api/v1/product-promotions", "POST", "PRODUCT PROMOTIONS", false));
        perms.add(new PermDef("Get assigned products", "/api/v1/promotions/{id}/products", "GET", "PROMOTIONS", false));
        perms.add(new PermDef("Assign products to promotion", "/api/v1/promotions/{id}/products", "POST", "PROMOTIONS", false));
        perms.add(new PermDef("Assign all products to promotion", "/api/v1/promotions/{id}/products/all", "POST", "PROMOTIONS", false));

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

        // ORDER
        perms.add(new PermDef("Create a order", "/api/v1/order/checkout", "POST", "ORDER", true));
        perms.add(new PermDef("Get order by id", "/api/v1/order/{id}", "GET", "ORDER", true));
        perms.add(new PermDef("Get my orders with pagination", "/api/v1/order/me", "GET", "ORDER", true));
        perms.add(new PermDef("Admin get all orders", "/api/v1/order/admin/all", "GET", "ORDER", false));
        perms.add(new PermDef("Update order status", "/api/v1/order/{id}/status", "PUT", "ORDER", false));
        perms.add(new PermDef("Bulk update order status", "/api/v1/order/bulk-status", "POST", "ORDER", false));

        // PAYMENT
        perms.add(new PermDef("Confirm payment", "/api/v1/payment/confirm", "POST", "PAYMENT", true));
        
        // PRICING
        perms.add(new PermDef("Get Product price", "/api/v1/price/{id}", "GET", "PRICING PRODUCT", true));

        // REVIEWS
        perms.add(new PermDef("Create a review", "/api/v1/reviews", "POST", "REVIEWS", true));
        perms.add(new PermDef("Get reviews by product", "/api/v1/reviews/product/{productId}", "GET", "REVIEWS", true));
        perms.add(new PermDef("Delete a review", "/api/v1/reviews/{id}", "DELETE", "REVIEWS", true));

        // CHAT
        perms.add(new PermDef("Send chat message", "/api/v1/chat", "POST", "CHAT", true));
        perms.add(new PermDef("Get chat history", "/api/v1/chat/history", "GET", "CHAT", true));

        boolean updated = false;
        for (PermDef def : perms) {
            Permission p = this.permissionRepository.findByModuleAndApiPathAndMethod(def.module, def.path, def.method);
            if (p == null) {
                p = new Permission(def.name, def.path, def.method, def.module);
                p = this.permissionRepository.save(p);
                System.out.println(">>> CREATED Permission: " + def.name);
            }

            final Permission finalP = p;
            if (adminRole != null) {
                if (adminRole.getPermissions() == null) {
                    adminRole.setPermissions(new ArrayList<>());
                }
                if (adminRole.getPermissions().stream().noneMatch(x -> x != null && x.getId().equals(finalP.getId()))) {
                    adminRole.getPermissions().add(p);
                    updated = true;
                    System.out.println(">>> ASSIGNED Permission: " + def.name + " TO SUPER_ADMIN");
                }
            }

            if (def.toUser && userRole != null) {
                if (userRole.getPermissions() == null) {
                    userRole.setPermissions(new ArrayList<>());
                }
                if (userRole.getPermissions().stream().noneMatch(x -> x != null && x.getId().equals(finalP.getId()))) {
                    userRole.getPermissions().add(p);
                    updated = true;
                    System.out.println(">>> ASSIGNED Permission: " + def.name + " TO ROLE_USER");
                }
            }
        }

        if (updated) {
            if (adminRole != null) this.roleRepository.save(adminRole);
            if (userRole != null) this.roleRepository.save(userRole);
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
