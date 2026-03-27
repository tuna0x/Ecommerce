package com.tuna.ecommerce.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Role;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.auth.ReqRegisterDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.user.ResCreateUser;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.domain.response.user.ResUpdateUser;
import com.tuna.ecommerce.repository.UserRepository;

import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;


    public User handleCreate(User user){
        if (user.getRole() != null && user.getRole().getId() != 0) {
            Role role = this.roleService.fetchById(user.getRole().getId());
            user.setRole(role);
        } else {
            Role defaultRole = this.roleService.fetchByName("ROLE_USER");
            user.setRole(defaultRole);
        }
        return this.userRepository.save(user);
    }

    public User getUserById(Long id){
        Optional<User> user=this.userRepository.findById(id);
        return user.isPresent() ? user.get() : null;
    }

    public User handleUpdate(User user){
        User curUser =getUserById(user.getId());
        if (curUser !=null) {
            curUser.setId(user.getId());
            curUser.setName(user.getName());
            curUser.setAddress(user.getAddress());
            curUser.setEmail(user.getEmail());
            curUser.setGender(user.getGender());
            curUser.setPassword(user.getPassword());

            if (user.getRole()!=null) {
            Role role = this.roleService.fetchById(user.getRole().getId());
            curUser.setRole(role);
        }

            user=this.userRepository.save(curUser);
        }
        return user;
    }

    public ResultPaginationDTO handleGetAll(Specification<User> spec, Pageable page){
        Page<User> user= this.userRepository.findAll(spec, page);
        ResultPaginationDTO rs=new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta=new ResultPaginationDTO.Meta();
        meta.setPage(user.getNumber() + 1);
        meta.setPageSize(user.getSize());
        meta.setPages(user.getTotalPages());
        meta.setTotal(user.getTotalElements());

        List<ResFetchUser> list=user.getContent().stream().map(item->this.convertToResFetchUser(item)).collect(Collectors.toList());

        rs.setMeta(meta);
        rs.setResult(list);
        return rs;
    }

    public void handleDelete(Long id){
        this.userRepository.deleteById(id);
    }

    public List<User> handleGetAllUsers() {
        return this.userRepository.findAll();
    }
    public ResCreateUser convertToResCreateUser(User user){
        ResCreateUser res=new ResCreateUser();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAddress(user.getAddress());
        res.setEmail(user.getEmail());
        res.setGender(user.getGender());
        res.setAge(user.getAge());
        return res;
    }
    public ResUpdateUser convertToResUpdateUser(User user){
        ResUpdateUser res= new ResUpdateUser();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAddress(user.getAddress());
        res.setEmail(user.getEmail());
        res.setGender(user.getGender());
        res.setAge(user.getAge());
        return res;
    }
    public ResFetchUser convertToResFetchUser(User user){
        ResFetchUser res=new ResFetchUser();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAddress(user.getAddress());
        res.setEmail(user.getEmail());
        res.setGender(user.getGender());
        res.setAge(user.getAge());
        return res;
    }
    public boolean exitsByEmail(String email){
        return this.userRepository.existsByEmail(email);
    }
    public User findByUsername(String email){
        return this.userRepository.findByEmail(email);
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.findByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token,email);
    }

    public User register(ReqRegisterDTO req){
        User user=new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());
        
        Role userRole = this.roleService.fetchByName("ROLE_USER");
        if (userRole != null) {
            user.setRole(userRole);
        }
        
        return this.userRepository.save(user);
    }
}
