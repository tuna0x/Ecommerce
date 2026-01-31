package com.tuna.ecommerce.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.Permission;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.PermissionRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;


        public boolean isPermissionExist(Permission p) {
        return this.permissionRepository.existsByModuleAndApiPathAndMethod(
                p.getModule(),
                p.getApiPath(),
                p.getMethod());
    }

    public boolean isSameName(Permission p) {
        Permission permissionDB = this.fetchById(p.getId());
        if (permissionDB != null) {
            if (permissionDB.getName().equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    public Permission create(Permission permission) {
        return this.permissionRepository.save(permission);
    }

    public Permission fetchById(long id) {
        Optional<Permission> p = this.permissionRepository.findById(id);
        if (p.isPresent()) {
            return p.get();
        }
        return null;
    }

    public Permission update(Permission p) {
        Permission permissionDB = this.fetchById(p.getId());
        if (permissionDB != null) {
            permissionDB.setName(p.getName());
            permissionDB.setApiPath(p.getApiPath());
            permissionDB.setMethod(p.getMethod());
            permissionDB.setModule(p.getModule());

            permissionDB = this.permissionRepository.save(permissionDB);
            return permissionDB;
        }
        return null;
    }

    public void delete(long id) {
        // Delete permission_role
        Optional<Permission> permissionOpt = this.permissionRepository.findById(id);
        Permission currentP = permissionOpt.get();
        if (currentP.getRoles() != null) {
            currentP.getRoles().forEach(role -> role.getPermissions().remove(currentP));
        }
        // Delete permission
        this.permissionRepository.delete(currentP);
    }

    public ResultPaginationDTO getAll(Specification<Permission> spec, Pageable pageable) {
        Page<Permission> pagePermission = this.permissionRepository.findAll(spec, pageable);
        ResultPaginationDTO resultPaginationDTO = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pagePermission.getNumber() + 1);
        meta.setPageSize(pagePermission.getSize());
        meta.setPages(pagePermission.getTotalPages());
        meta.setTotal(pagePermission.getTotalElements());

        resultPaginationDTO.setMeta(meta);

        resultPaginationDTO.setResult(pagePermission.getContent());

        return resultPaginationDTO;
    }
}
