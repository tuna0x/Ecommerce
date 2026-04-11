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
import com.tuna.ecommerce.domain.UserProfile;
import com.tuna.ecommerce.ultil.constant.GenderEnum;
import com.tuna.ecommerce.domain.request.auth.ReqRegisterDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.user.ResCreateUser;
import com.tuna.ecommerce.domain.response.user.ResFetchUser;
import com.tuna.ecommerce.domain.response.user.ResUpdateUser;
import com.tuna.ecommerce.domain.request.user.ReqUpdateUserDTO;
import com.tuna.ecommerce.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;

    public User handleCreate(User user) {
        if (user.getRole() != null && user.getRole().getId() != 0) {
            Role role = this.roleService.fetchById(user.getRole().getId());
            user.setRole(role);
        } else {
            Role defaultRole = this.roleService.fetchByName("ROLE_USER");
            user.setRole(defaultRole);
        }

        // Initialize UserProfile if not present
        if (user.getUserProfile() == null) {
            UserProfile profile = new UserProfile();
            profile.setName("User_" + user.getEmail().split("@")[0]);
            profile.setUser(user);
            user.setUserProfile(profile);
        } else {
            user.getUserProfile().setUser(user);
        }

        return this.userRepository.save(user);
    }

    public User getUserById(Long id) {
        if (id == null)
            return null;
        return this.userRepository.findById(id).orElse(null);
    }

    public User handleUpdate(User user) {
        User curUser = getUserById(user.getId());
        if (curUser != null) {
            curUser.setEmail(user.getEmail());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                curUser.setPassword(user.getPassword());
            }

            if (user.getUserProfile() != null) {
                UserProfile curProfile = curUser.getUserProfile();
                if (user.getUserProfile().getName() != null)
                    curProfile.setName(user.getUserProfile().getName());
                if (user.getUserProfile().getAge() != 0)
                    curProfile.setAge(user.getUserProfile().getAge());
                if (user.getUserProfile().getGender() != null) {
                    try {
                        curProfile.setGender(user.getUserProfile().getGender());
                    } catch (Exception e) {
                        // Handle potential enum mapping issues if needed
                    }
                }
                if (user.getUserProfile().getImage() != null)
                    curProfile.setImage(user.getUserProfile().getImage());
            }

            if (user.getRole() != null) {
                Role role = this.roleService.fetchById(user.getRole().getId());
                curUser.setRole(role);
            }

            curUser = this.userRepository.save(curUser);
        }
        return curUser;
    }

    public User handleUpdateProfile(ReqUpdateUserDTO req) {
        User curUser = getUserById(req.getId());
        if (curUser != null) {
            UserProfile curProfile = curUser.getUserProfile();
            if (curProfile == null) {
                curProfile = new UserProfile();
                curProfile.setUser(curUser);
                curUser.setUserProfile(curProfile);
            }

            if (req.getName() != null)
                curProfile.setName(req.getName());
            if (req.getAge() != null)
                curProfile.setAge(req.getAge());
            if (req.getGender() != null) {
                try {
                    curProfile.setGender(GenderEnum.valueOf(req.getGender().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Ignore or handle invalid gender string
                }
            }
            if (req.getImage() != null)
                curProfile.setImage(req.getImage());

            curUser = this.userRepository.save(curUser);
        }
        return curUser;
    }

    public ResultPaginationDTO handleGetAll(Specification<User> spec, Pageable page) {
        Page<User> user = this.userRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(user.getNumber() + 1);
        meta.setPageSize(user.getSize());
        meta.setPages(user.getTotalPages());
        meta.setTotal(user.getTotalElements());

        List<ResFetchUser> list = user.getContent().stream()
                .map(item -> this.convertToResFetchUser(item))
                .collect(Collectors.toList());

        rs.setMeta(meta);
        rs.setResult(list);
        return rs;
    }

    public void handleDelete(Long id) {
        this.userRepository.deleteById(id);
    }

    public List<User> handleGetAllUsers() {
        return this.userRepository.findAll();
    }

    public ResCreateUser convertToResCreateUser(User user) {
        ResCreateUser res = new ResCreateUser();
        res.setId(user.getId());
        res.setEmail(user.getEmail());

        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            res.setName(profile.getName());
            res.setGender(profile.getGender());
            res.setAge(profile.getAge());
            res.setImage(profile.getImage());
        }

        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedBy(user.getCreatedBy());
        res.setUpdateBy(user.getUpdatedBy());
        res.setActive(user.getActive());

        if (user.getRole() != null) {
            ResCreateUser.RoleUser roleRes = new ResCreateUser.RoleUser();
            roleRes.setId(user.getRole().getId());
            roleRes.setName(user.getRole().getName());
            res.setRole(roleRes);
        }
        return res;
    }

    public ResUpdateUser convertToResUpdateUser(User user) {
        ResUpdateUser res = new ResUpdateUser();
        res.setId(user.getId());
        res.setEmail(user.getEmail());

        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            res.setName(profile.getName());
            res.setGender(profile.getGender());
            res.setAge(profile.getAge());
            res.setImage(profile.getImage());
        }

        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedBy(user.getCreatedBy());
        res.setUpdateBy(user.getUpdatedBy());
        res.setActive(user.getActive());

        if (user.getRole() != null) {
            ResUpdateUser.RoleUser roleRes = new ResUpdateUser.RoleUser();
            roleRes.setId(user.getRole().getId());
            roleRes.setName(user.getRole().getName());
            res.setRole(roleRes);
        }
        return res;
    }

    public ResFetchUser convertToResFetchUser(User user) {
        ResFetchUser res = new ResFetchUser();
        res.setId(user.getId());
        res.setEmail(user.getEmail());

        UserProfile profile = user.getUserProfile();
        if (profile != null) {
            res.setName(profile.getName());
            res.setGender(profile.getGender());
            res.setAge(profile.getAge());
            res.setImage(profile.getImage());
        }

        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedBy(user.getCreatedBy());
        res.setUpdateBy(user.getUpdatedBy());
        res.setActive(user.getActive());

        if (user.getRole() != null) {
            ResFetchUser.RoleUser roleRes = new ResFetchUser.RoleUser();
            roleRes.setId(user.getRole().getId());
            roleRes.setName(user.getRole().getName());
            res.setRole(roleRes);
        }
        return res;
    }

    public User handleToggleActive(Long id, boolean active) {
        User user = this.getUserById(id);
        if (user != null) {
            user.setActive(active);
            user = this.userRepository.save(user);
        }
        return user;
    }

    public User handleUpdateRole(Long id, Long roleId) {
        User user = this.getUserById(id);
        if (user != null) {
            Role role = this.roleService.fetchById(roleId);
            if (role != null) {
                user.setRole(role);
                user = this.userRepository.save(user);
            }
        }
        return user;
    }

    public boolean exitsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public User findByUsername(String email) {
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
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    public User register(ReqRegisterDTO req) {
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());

        UserProfile profile = new UserProfile();
        profile.setName(req.getName());
        profile.setUser(user);
        user.setUserProfile(profile);

        Role userRole = this.roleService.fetchByName("ROLE_USER");
        if (userRole != null) {
            user.setRole(userRole);
        }

        return this.userRepository.save(user);
    }
}
