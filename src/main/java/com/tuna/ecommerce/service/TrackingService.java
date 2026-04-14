package com.tuna.ecommerce.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserActivityLog;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.UserActivityLogRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.constant.ActionTypeEnum;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;

    @Async
    public void logActivity(String email, String ip, String actionTypeStr, String metadata) {
        try {
            ActionTypeEnum actionType;
            try {
                actionType = ActionTypeEnum.valueOf(actionTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return; // Ignore invalid types
            }

            UserActivityLog log = new UserActivityLog();
            log.setUserEmail(email);
            log.setIpAddress(ip);
            log.setActionType(actionType);
            log.setMetadata(metadata);

            if (email != null && !email.equals("anonymous")) {
                User user = userRepository.findByEmail(email);
                log.setUser(user);
            }

            userActivityLogRepository.save(log);
        } catch (Exception e) {
            // Silently fail, tracking should not break the app
            System.err.println("Failed to log activity: " + e.getMessage());
        }
    }

    public ResultPaginationDTO handleGetAll(Specification<UserActivityLog> spec, Pageable pageable) {
        Page<UserActivityLog> pLogs = this.userActivityLogRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pLogs.getTotalPages());
        mt.setTotal(pLogs.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pLogs.getContent());

        return rs;
    }
}
