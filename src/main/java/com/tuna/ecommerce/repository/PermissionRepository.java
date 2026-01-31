package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission,Long>,JpaSpecificationExecutor<Permission>{

    boolean existsByModuleAndApiPathAndMethod(String module, String apiPath, String method);

    List<Permission> findByIdIn(List<Long> idList);

}