package com.tuna.ecommerce.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import com.tuna.ecommerce.domain.Permission;
import com.tuna.ecommerce.domain.Role;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserProfile;
import com.tuna.ecommerce.repository.BannerRepository;
import com.tuna.ecommerce.repository.BrandRepository;
import com.tuna.ecommerce.repository.PermissionRepository;
import com.tuna.ecommerce.repository.RoleRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.constant.GenderEnum;

@Service
public class DatabaseInitializer implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrandRepository brandRepository;
    private final BannerRepository bannerRepository;

    public DatabaseInitializer(PermissionRepository permissionRepository, RoleRepository roleRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder, BrandRepository brandRepository,
            BannerRepository bannerRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.brandRepository = brandRepository;
        this.bannerRepository = bannerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> START INIT DATABASE");

        long countPermissions = this.permissionRepository.count();
        long countRoles = this.roleRepository.count();
        long countUsers = this.userRepository.count();

        if (countPermissions == 0) {
            ArrayList<Permission> arr = new ArrayList<>();

            // Permissions for Administration
            arr.add(new Permission("Create a permission", "/api/v1/permissions", "POST", "PERMISSIONS"));
            arr.add(new Permission("Update a permission", "/api/v1/permissions", "PUT", "PERMISSIONS"));
            arr.add(new Permission("Delete a permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS"));
            arr.add(new Permission("Get a permission by id", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"));
            arr.add(new Permission("Get permissions with pagination", "/api/v1/permissions", "GET", "PERMISSIONS"));

            arr.add(new Permission("Create a role", "/api/v1/roles", "POST", "ROLES"));
            arr.add(new Permission("Update a role", "/api/v1/roles", "PUT", "ROLES"));
            arr.add(new Permission("Delete a role", "/api/v1/roles/{id}", "DELETE", "ROLES"));
            arr.add(new Permission("Get a role by id", "/api/v1/roles/{id}", "GET", "ROLES"));
            arr.add(new Permission("Get roles with pagination", "/api/v1/roles", "GET", "ROLES"));

            arr.add(new Permission("Create a user", "/api/v1/users", "POST", "USERS"));
            arr.add(new Permission("Update a user", "/api/v1/users", "PUT", "USERS"));
            arr.add(new Permission("Delete a user", "/api/v1/users/{id}", "DELETE", "USERS"));
            arr.add(new Permission("Get a user by id", "/api/v1/users/{id}", "GET", "USERS"));
            arr.add(new Permission("Get users with pagination", "/api/v1/users", "GET", "USERS"));

            arr.add(new Permission("Get dashboard statistics", "/api/v1/dashboard/statistics", "GET", "DASHBOARD"));

            // Catalog Management (Admin Only for POST/PUT/DELETE)
            arr.add(new Permission("Create a product", "/api/v1/products", "POST", "PRODUCTS"));
            arr.add(new Permission("Update a product", "/api/v1/products", "PUT", "PRODUCTS"));
            arr.add(new Permission("Delete a product", "/api/v1/products/{id}", "DELETE", "PRODUCTS"));
            arr.add(new Permission("Get a product by id", "/api/v1/products/{id}", "GET", "PRODUCTS"));
            arr.add(new Permission("Get products with pagination", "/api/v1/products", "GET", "PRODUCTS"));
            arr.add(new Permission("Get related products", "/api/v1/products/{id}/related", "GET", "PRODUCTS"));

            arr.add(new Permission("Create a category", "/api/v1/categories", "POST", "CATEGORIES"));
            arr.add(new Permission("Update a category", "/api/v1/categories", "PUT", "CATEGORIES"));
            arr.add(new Permission("Delete a category", "/api/v1/categories/{id}", "DELETE", "CATEGORIES"));
            arr.add(new Permission("Get a category by id", "/api/v1/categories/{id}", "GET", "CATEGORIES"));
            arr.add(new Permission("Get categories with pagination", "/api/v1/categories", "GET", "CATEGORIES"));

            arr.add(new Permission("Create a brand", "/api/v1/brands", "POST", "BRANDS"));
            arr.add(new Permission("Update a brand", "/api/v1/brands", "PUT", "BRANDS"));
            arr.add(new Permission("Delete a brand", "/api/v1/brands/{id}", "DELETE", "BRANDS"));
            arr.add(new Permission("Get a brand by id", "/api/v1/brands/{id}", "GET", "BRANDS"));
            arr.add(new Permission("Get brands with pagination", "/api/v1/brands", "GET", "BRANDS"));

            arr.add(new Permission("Create a attribute", "/api/v1/attributes", "POST", "ATTRIBUTES"));
            arr.add(new Permission("Update a attribute", "/api/v1/attributes", "PUT", "ATTRIBUTES"));
            arr.add(new Permission("Delete a attribute", "/api/v1/attributes/{id}", "DELETE", "ATTRIBUTES"));
            arr.add(new Permission("Get a attribute by id", "/api/v1/attributes/{id}", "GET", "ATTRIBUTES"));
            arr.add(new Permission("Get attributes with pagination", "/api/v1/attributes", "GET", "ATTRIBUTES"));

            arr.add(new Permission("Create a banner", "/api/v1/banners", "POST", "BANNERS"));
            arr.add(new Permission("Update a banner", "/api/v1/banners", "PUT", "BANNERS"));
            arr.add(new Permission("Delete a banner", "/api/v1/banners/{id}", "DELETE", "BANNERS"));
            arr.add(new Permission("Get a banner by id", "/api/v1/banners/{id}", "GET", "BANNERS"));
            arr.add(new Permission("Get banners with pagination", "/api/v1/banners", "GET", "BANNERS"));
            arr.add(new Permission("Toggle banner active status", "/api/v1/banners/{id}/active", "PATCH", "BANNERS"));

            arr.add(new Permission("Create a attribute value", "/api/v1/attributes-values", "POST", "ATTRIBUTE VALUE"));
            arr.add(new Permission("Update a attribute value", "/api/v1/attributes-values", "PUT", "ATTRIBUTE VALUE"));
            arr.add(new Permission("Delete a attribute value", "/api/v1/attributes-values/{id}", "DELETE",
                    "ATTRIBUTE VALUE"));
            arr.add(new Permission("Get a attribute value by id", "/api/v1/attributes-values/{id}", "GET",
                    "ATTRIBUTE VALUE"));
            arr.add(new Permission("Get attribute values with pagination", "/api/v1/attributes-values", "GET",
                    "ATTRIBUTE VALUE"));

            arr.add(new Permission("Create a product detail", "/api/v1/product-detail", "POST", "PRODUCT DETAIL"));
            arr.add(new Permission("Update a product detail", "/api/v1/product-detail", "PUT", "PRODUCT DETAIL"));
            arr.add(new Permission("Delete a product detail", "/api/v1/product-detail/{id}", "DELETE",
                    "PRODUCT DETAIL"));
            arr.add(new Permission("Get a product detail by id", "/api/v1/product-detail/{id}", "GET",
                    "PRODUCT DETAIL"));
            arr.add(new Permission("Get product-detail with pagination", "/api/v1/product-detail", "GET",
                    "PRODUCT DETAIL"));

            // Coupons & Promotions
            arr.add(new Permission("Create a coupon", "/api/v1/coupons", "POST", "COUPONS"));
            arr.add(new Permission("Update a coupon", "/api/v1/coupons", "PUT", "COUPONS"));
            arr.add(new Permission("Delete a coupon", "/api/v1/coupons/{id}", "DELETE", "COUPONS"));
            arr.add(new Permission("Get a coupon by id", "/api/v1/coupons/{id}", "GET", "COUPONS"));
            arr.add(new Permission("Get coupons with pagination", "/api/v1/coupons", "GET", "COUPONS"));

            arr.add(new Permission("Create a promotion", "/api/v1/promotions", "POST", "PROMOTIONS"));
            arr.add(new Permission("Update a promotion", "/api/v1/promotions", "PUT", "PROMOTIONS"));
            arr.add(new Permission("Delete a promotion", "/api/v1/promotions/{id}", "DELETE", "PROMOTIONS"));
            arr.add(new Permission("Get a promotion by id", "/api/v1/promotions/{id}", "GET", "PROMOTIONS"));
            arr.add(new Permission("Get promotions with pagination", "/api/v1/promotions", "GET", "PROMOTIONS"));
            arr.add(new Permission("Active a promotion", "/api/v1/promotions/active/{id}", "POST", "PROMOTIONS"));
            arr.add(new Permission("Deactive a promotion", "/api/v1/promotions/deactive/{id}", "POST", "PROMOTIONS"));
            arr.add(new Permission("Assign promotion to product", "/api/v1/product-promotions", "POST",
                    "PRODUCT PROMOTIONS"));

            arr.add(new Permission("Get assigned products", "/api/v1/promotions/{id}/products", "GET", "PROMOTIONS"));
            arr.add(new Permission("Assign products to promotion", "/api/v1/promotions/{id}/products", "POST",
                    "PROMOTIONS"));
            arr.add(new Permission("Assign all products to promotion", "/api/v1/promotions/{id}/products/all", "POST",
                    "PROMOTIONS"));

            // User Operations (Common for Admin & User)
            arr.add(new Permission("Create a address", "/api/v1/addresses", "POST", "ADDRESSES"));
            arr.add(new Permission("Update a address", "/api/v1/addresses", "PUT", "ADDRESSES"));
            arr.add(new Permission("Delete a address", "/api/v1/addresses/{id}", "DELETE", "ADDRESSES"));
            arr.add(new Permission("Set address default", "/api/v1/addresses/{id}/default", "PUT", "ADDRESSES"));
            arr.add(new Permission("Get addresses with pagination", "/api/v1/addresses", "GET", "ADDRESSES"));

            arr.add(new Permission("Add to cart", "/api/v1/cart", "POST", "CART"));
            arr.add(new Permission("Get a cart", "/api/v1/cart", "GET", "CART"));
            arr.add(new Permission("Update item in cart", "/api/v1/cart", "PUT", "CART"));
            arr.add(new Permission("Delete item in cart by id", "/api/v1/cart/{id}", "DELETE", "CART"));

            arr.add(new Permission("Create a order", "/api/v1/order/checkout", "POST", "ORDER"));
            arr.add(new Permission("Get order by id", "/api/v1/order/{id}", "GET", "ORDER"));

            arr.add(new Permission("Confirm payment", "/api/v1/payment/confirm", "POST", "PAYMENT"));
            arr.add(new Permission("Get Product price", "/api/v1/price/{id}", "GET", "PRICING PRODUCT"));

            arr.add(new Permission("Create a review", "/api/v1/reviews", "POST", "REVIEWS"));
            arr.add(new Permission("Get reviews by product", "/api/v1/reviews/product/{productId}", "GET", "REVIEWS"));
            arr.add(new Permission("Delete a review", "/api/v1/reviews/{id}", "DELETE", "REVIEWS"));

            arr.add(new Permission("Send chat message", "/api/v1/chat", "POST", "CHAT"));
            arr.add(new Permission("Get chat history", "/api/v1/chat/history", "GET", "CHAT"));

            this.permissionRepository.saveAll(arr);
        }

        if (countRoles == 0) {
            List<Permission> allPermissions = this.permissionRepository.findAll();

            // 1. SUPER_ADMIN Role
            Role adminRole = new Role();
            adminRole.setName("SUPER_ADMIN");
            adminRole.setDescription("Full control role");
            adminRole.setActive(true);
            adminRole.setPermissions(allPermissions);
            this.roleRepository.save(adminRole);

            // 2. USER Role (Only essential permissions)
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole.setDescription("Regular customer role");
            userRole.setActive(true);

            // Define permissions for USER
            List<Permission> userPermissions = allPermissions.stream().filter(p -> {
                String method = p.getMethod();
                String url = p.getApiPath();

                // USER can GET products, categories, brands, attributes, etc.
                boolean isGetPublic = method.equals("GET") && (url.startsWith("/api/v1/products") ||
                        url.startsWith("/api/v1/categories") ||
                        url.startsWith("/api/v1/brands") ||
                        url.startsWith("/api/v1/attributes") ||
                        url.startsWith("/api/v1/product-detail") ||
                        url.startsWith("/api/v1/price") ||
                        url.startsWith("/api/v1/banners") ||
                        url.startsWith("/api/v1/reviews/product"));

                // USER has full access to Cart, Address, Order, Payment Confirm
                boolean isUserOwnData = url.startsWith("/api/v1/cart") ||
                        url.startsWith("/api/v1/addresses") ||
                        url.startsWith("/api/v1/order") ||
                        url.startsWith("/api/v1/payment") ||
                        url.startsWith("/api/v1/reviews");

                return isGetPublic || isUserOwnData;
            }).collect(Collectors.toList());

            userRole.setPermissions(userPermissions);
            this.roleRepository.save(userRole);
        }

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

            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null)
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

            Role userRole = this.roleRepository.findByName("ROLE_USER");
            if (userRole != null)
                normalUser.setRole(userRole);
            this.userRepository.save(normalUser);
        }

        // Sync extra permissions even if table not empty
        boolean isExistToggleBanner = this.permissionRepository.existsByModuleAndApiPathAndMethod("BANNERS",
                "/api/v1/banners/{id}/active", "PATCH");
        if (!isExistToggleBanner) {
            Permission p = new Permission("Toggle banner active status", "/api/v1/banners/{id}/active", "PATCH",
                    "BANNERS");
            this.permissionRepository.save(p);

            // Add to SUPER_ADMIN role
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminRole.getPermissions().add(p);
                this.roleRepository.save(adminRole);
                System.out.println(">>> ADDED Toggle Banner Permission TO SUPER_ADMIN");
            }
        }

        boolean isExistToggleUser = this.permissionRepository.existsByModuleAndApiPathAndMethod("USERS",
                "/api/v1/users/{id}/active", "PATCH");
        if (!isExistToggleUser) {
            Permission p = new Permission("Toggle user active status", "/api/v1/users/{id}/active", "PATCH", "USERS");
            this.permissionRepository.save(p);
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminRole.getPermissions().add(p);
                this.roleRepository.save(adminRole);
                System.out.println(">>> ADDED Toggle User Permission TO SUPER_ADMIN");
            }
        }

        boolean isExistUpdateRole = this.permissionRepository.existsByModuleAndApiPathAndMethod("USERS",
                "/api/v1/users/{id}/role", "PATCH");
        if (!isExistUpdateRole) {
            Permission p = new Permission("Update user role", "/api/v1/users/{id}/role", "PATCH", "USERS");
            this.permissionRepository.save(p);
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminRole.getPermissions().add(p);
                this.roleRepository.save(adminRole);
                System.out.println(">>> ADDED Update Role Permission TO SUPER_ADMIN");
            }
        }

        // Sync Promotion PATCH active
        boolean isExistTogglePromo = this.permissionRepository.existsByModuleAndApiPathAndMethod("PROMOTIONS",
                "/api/v1/promotions/{id}/active", "PATCH");
        if (!isExistTogglePromo) {
            Permission p = new Permission("Toggle promotion active status", "/api/v1/promotions/{id}/active", "PATCH",
                    "PROMOTIONS");
            this.permissionRepository.save(p);
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminRole.getPermissions().add(p);
                this.roleRepository.save(adminRole);
            }
        }

        // Sync Coupon PATCH active & DELETE
        boolean isExistToggleCoupon = this.permissionRepository.existsByModuleAndApiPathAndMethod("COUPONS",
                "/api/v1/coupons/{id}/active", "PATCH");
        if (!isExistToggleCoupon) {
            Permission p = new Permission("Toggle coupon active status", "/api/v1/coupons/{id}/active", "PATCH",
                    "COUPONS");
            this.permissionRepository.save(p);
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminRole.getPermissions().add(p);
                this.roleRepository.save(adminRole);
            }
        }

        boolean isExistDeleteCoupon = this.permissionRepository.existsByModuleAndApiPathAndMethod("COUPONS",
                "/api/v1/coupons/{id}", "DELETE");
        if (!isExistDeleteCoupon) {
            Permission p = new Permission("Delete a coupon", "/api/v1/coupons/{id}", "DELETE", "COUPONS");
            this.permissionRepository.save(p);
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminRole.getPermissions().add(p);
                this.roleRepository.save(adminRole);
            }
        }

        // Sync Promotion Product Management
        String[][] promoProductPerms = {
                { "Get assigned products", "/api/v1/promotions/{id}/products", "GET" },
                { "Assign products to promotion", "/api/v1/promotions/{id}/products", "POST" },
                { "Assign all products to promotion", "/api/v1/promotions/{id}/products/all", "POST" }
        };

        for (String[] perm : promoProductPerms) {
            boolean exists = this.permissionRepository.existsByModuleAndApiPathAndMethod("PROMOTIONS", perm[1], perm[2]);
            if (!exists) {
                Permission p = new Permission(perm[0], perm[1], perm[2], "PROMOTIONS");
                this.permissionRepository.save(p);
                Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
                if (adminRole != null) {
                    adminRole.getPermissions().add(p);
                    this.roleRepository.save(adminRole);
                }
            }
        }
    }
}
