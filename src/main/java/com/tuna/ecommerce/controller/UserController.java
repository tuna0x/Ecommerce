package com.tuna.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.user.ResCreateUser;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.domain.response.user.ResUpdateUser;
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
    @APIMessage("Update user successfully")
    public ResponseEntity<ResUpdateUser> updateUser(@RequestBody User user) throws IdInvalidException {
        if (this.userService.getUserById(user.getId())==null) {
            throw new IdInvalidException("user is valid");
        }
        User cur=this.userService.handleCreate(user);
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
    public ResponseEntity<ResultPaginationDTO> getAllUser(@Filter Specification<User> spec,Pageable page) {
        return ResponseEntity.ok().body(this.userService.handleGetAll(spec, page));
    }


}
