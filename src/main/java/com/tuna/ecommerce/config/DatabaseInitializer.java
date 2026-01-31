package com.tuna.ecommerce.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Permission;
import com.tuna.ecommerce.domain.Role;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.repository.PermissionRepository;
import com.tuna.ecommerce.repository.RoleRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.constant.GenderEnum;


@Service
public class DatabaseInitializer implements CommandLineRunner{
    private final PermissionRepository permissionrRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(PermissionRepository permissionrRepository, RoleRepository roleRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.permissionrRepository = permissionrRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> START INIT DATABASE");

        long countPermissions = this.permissionrRepository.count();
        long countRoles = this.roleRepository.count();
        long countUsers = this.userRepository.count();

        if (countPermissions == 0) {
            ArrayList<Permission> arr=new ArrayList<>();

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

            arr.add(new Permission("Create a product", "/api/v1/products", "POST", "PRODUCTS"));
            arr.add(new Permission("Update a product", "/api/v1/products", "PUT", "PRODUCTS"));
            arr.add(new Permission("Delete a product", "/api/v1/products/{id}", "DELETE", "PRODUCTS"));
            arr.add(new Permission("Get a product by id", "/api/v1/products/{id}", "GET", "PRODUCTS"));
            arr.add(new Permission("Get products with pagination", "/api/v1/products", "GET", "PRODUCTS"));

            arr.add(new Permission("Create a category", "/api/v1/categories", "POST", "CATEGORIES"));
            arr.add(new Permission("Update a category", "/api/v1/categories", "PUT", "CATEGORIES"));
            arr.add(new Permission("Delete a category", "/api/v1/categories/{id}", "DELETE", "CATEGORIES"));
            arr.add(new Permission("Get a category by id", "/api/v1/categories/{id}", "GET", "CATEGORIES"));
            arr.add(new Permission("Get categories with pagination", "/api/v1/categories", "GET", "CATEGORIES"));

            arr.add(new Permission("Create a attribute", "/api/v1/attributes", "POST", "ATTRIBUTES"));
            arr.add(new Permission("Update a attribute", "/api/v1/attributes", "PUT", "ATTRIBUTES"));
            arr.add(new Permission("Delete a attribute", "/api/v1/attributes/{id}", "DELETE", "ATTRIBUTES"));
            arr.add(new Permission("Get a attribute by id", "/api/v1/attributes/{id}", "GET", "ATTRIBUTES"));
            arr.add(new Permission("Get attributes with pagination", "/api/v1/attributes", "GET", "ATTRIBUTES"));

            arr.add(new Permission("Create a attribute value", "/api/v1/attributes-values", "POST", "ATTRIBUTE VALUE"));
            arr.add(new Permission("Update a attribute value", "/api/v1/attributes-values", "PUT", "ATTRIBUTE VALUE"));
            arr.add(new Permission("Delete a attribute value", "/api/v1/attributes-values/{id}", "DELETE", "ATTRIBUTE VALUE"));
            arr.add(new Permission("Get a attribute value by id", "/api/v1/attributes-values/{id}", "GET", "ATTRIBUTE VALUE"));
            arr.add(new Permission("Get attribute values with pagination", "/api/v1/attributes-values", "GET", "ATTRIBUTE VALUE"));

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

            arr.add(new Permission("Add to cart", "/api/v1/cart", "POST", "CART"));
            arr.add(new Permission("Get a cart", "/api/v1/cart", "GET", "CART"));
            arr.add(new Permission("Update item in cart", "/api/v1/cart", "PUT", "CART"));
            arr.add(new Permission("Delete item in cart by id", "/api/v1/cart/{id}", "DELETE", "CART"));

            arr.add(new Permission("Create a order", "/api/v1/order/checkout", "POST", "ORDER"));
            arr.add(new Permission("Get order by id", "/api/v1/order/{id}", "GET", "ORDER"));

            arr.add(new Permission("Confirm payment", "/api/v1/payment/confirm", "POST", "PAYMENT"));

            arr.add(new Permission("Assign promotion to product", "/api/v1/product-promotions", "POST", "PRODUCT PROMOTIONS"));
            arr.add(new Permission("Get Product price", "/api/v1/price/{id}", "GET", "PRICING PRODUCT"));

            this.permissionrRepository.saveAll(arr);
        }

        if (countRoles == 0) {
            List<Permission> allPermissions = this.permissionrRepository.findAll();
            Role adminRole = new Role();
            adminRole.setName("SUPER_ADMIN");
            adminRole.setDescription("admin is full permissions");
            adminRole.setActive(true);
            adminRole.setPermissions(allPermissions);

            this.roleRepository.save(adminRole);
        }

        if (countUsers == 0) {
            User admin=new User();
            admin.setEmail("admin@gmail.com");
            admin.setAddress("HA NOI");
            admin.setAge(20);
            admin.setGender(GenderEnum.MALE);
            admin.setName("SUPPER ADMIN");
            admin.setPassword(this.passwordEncoder.encode("123456"));

            Role adminRole=this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                admin.setRole(adminRole);
            }
            this.userRepository.save(admin);
        }
    }
}
