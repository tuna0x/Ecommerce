package com.tuna.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.user.ResCreateUser;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.domain.response.user.ResUpdateUser;
import com.tuna.ecommerce.domain.request.user.ReqUpdateUserDTO;
import com.tuna.ecommerce.service.UserService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class UserController {
       private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/users")
    @APIMessage("Create user successfully")
    public ResponseEntity<ResCreateUser> createUser(@Valid @RequestBody User user) throws IdInvalidException {
        boolean check=this.userService.exitsByEmail(user.getEmail());
        if (check==true) {
            throw new IdInvalidException("email is exists");
        }
        String hashPassword = this.passwordEncoder.encode(user.getPassword());
        user.setPassword(hashPassword);
        User cur=this.userService.handleCreate(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUser(cur));
    }


    @PutMapping("/users")
    @APIMessage("Update user profile successfully")
    public ResponseEntity<ResUpdateUser> updateUser(
            @RequestPart("data") ReqUpdateUserDTO userDto,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IdInvalidException, IOException {
        if (this.userService.getUserById(userDto.getId()) == null) {
            throw new IdInvalidException("user id is invalid");
        }
        User cur = this.userService.handleUpdateProfile(userDto, file);
        return ResponseEntity.ok().body(this.userService.convertToResUpdateUser(cur));
    }

    @DeleteMapping("/users/{id}")
    @APIMessage("Delete user successfully")
    public ResponseEntity<Void> deleteUser(@PathVariable ("id") Long id) throws IdInvalidException{
        User user=this.userService.getUserById(id);
        if (user == null) {
            throw new IdInvalidException("Id invalid");
        }
        this.userService.handleDelete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/users/{id}")
    @APIMessage("Get user by id successfully")
    public ResponseEntity<ResFetchUser> getUserById(@PathVariable ("id") Long id) {
        User cur=this.userService.getUserById(id);
        return ResponseEntity.ok().body(this.userService.convertToResFetchUser(cur));
    }


    @GetMapping("/users")
    @APIMessage("Get all users successfully")
    public ResponseEntity<ResultPaginationDTO> getAllUser(@Filter Specification<User> spec, Pageable page) {
        return ResponseEntity.ok().body(this.userService.handleGetAll(spec, page));
    }

    @PatchMapping("/users/{id}/active")
    @APIMessage("Toggle user active status successfully")
    public ResponseEntity<ResUpdateUser> toggleActive(@PathVariable("id") Long id, @RequestBody User user)
            throws IdInvalidException {
        User cur = this.userService.handleToggleActive(id, user.getActive());
        if (cur == null) {
            throw new IdInvalidException("User id invalid");
        }
        return ResponseEntity.ok().body(this.userService.convertToResUpdateUser(cur));
    }

    @PatchMapping("/users/{id}/role")
    @APIMessage("Update user role successfully")
    public ResponseEntity<ResUpdateUser> updateRole(@PathVariable("id") Long id, @RequestBody User user)
            throws IdInvalidException {
        if (user.getRole() == null || user.getRole().getId() == 0) {
            throw new IdInvalidException("Role invalid");
        }
        User cur = this.userService.handleUpdateRole(id, user.getRole().getId());
        if (cur == null) {
            throw new IdInvalidException("User id invalid");
        }
        return ResponseEntity.ok().body(this.userService.convertToResUpdateUser(cur));
    }

    @GetMapping("/users/{id}/analytics")
    @PreAuthorize("@securityService.hasPermission('USERS', 'ANALYTICS')")
    @APIMessage("Lấy dữ liệu phân tích người dùng thành công")
    public ResponseEntity<com.tuna.ecommerce.domain.response.user.ResUserAnalyticsDTO> getUserAnalytics(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.userService.getUserAnalytics(id));
    }

    @PatchMapping("/users/{id}/admin-notes")
    @PreAuthorize("@securityService.hasPermission('USERS', 'UPDATE_NOTES')")
    @APIMessage("Cập nhật ghi chú Admin thành công")
    public ResponseEntity<User> updateAdminNotes(@PathVariable("id") Long id, @RequestBody java.util.Map<String, String> request) {
        String notes = request.get("notes");
        return ResponseEntity.ok(this.userService.handleUpdateAdminNotes(id, notes));
    }
}
