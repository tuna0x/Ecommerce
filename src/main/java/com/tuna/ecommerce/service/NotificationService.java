package com.tuna.ecommerce.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.Notification;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.NotificationRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification = this.notificationRepository.save(notification);

        // Gửi thông báo thời gian thực qua WebSocket
        // Destination: /user/{email}/queue/notifications
        this.messagingTemplate.convertAndSendToUser(
            user.getEmail().toLowerCase(), 
            "/queue/notifications", 
            notification
        );

        return notification;
    }

    public ResultPaginationDTO getNotificationsByUser(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userService.findByUsername(email);
        
        if (currentUser == null) {
            return null;
        }

        Page<Notification> pageNotification = this.notificationRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
        
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        
        meta.setPage(pageNotification.getNumber() + 1);
        meta.setPageSize(pageNotification.getSize());
        meta.setPages(pageNotification.getTotalPages());
        meta.setTotal(pageNotification.getTotalElements());
        
        rs.setMeta(meta);
        rs.setResult(pageNotification.getContent());
        
        return rs;
    }

    public Notification markAsRead(long id) {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userService.findByUsername(email);
        
        Optional<Notification> notificationOptional = this.notificationRepository.findById(id);
        if (notificationOptional.isPresent()) {
            Notification notification = notificationOptional.get();
            // Bảo mật: Chỉ cho phép người sở hữu thông báo đánh dấu là đã đọc
            if (currentUser != null && notification.getUser().getId().equals(currentUser.getId())) {
                notification.setRead(true);
                return this.notificationRepository.save(notification);
            }
        }
        return null;
    }

    public void markAllAsRead() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userService.findByUsername(email);

        if (currentUser != null) {
            List<Notification> unreadNotifications = this.notificationRepository.findByUserAndIsReadFalse(currentUser);
            unreadNotifications.forEach(n -> n.setRead(true));
            this.notificationRepository.saveAll(unreadNotifications);
        }
    }

    public void sendNotificationToAllUsers(String title, String message, String type) {
        List<User> users = this.userService.handleGetAllUsers();
        List<Notification> notifications = new java.util.ArrayList<>();
        
        for (User user : users) {
             Notification notification = new Notification();
             notification.setUser(user);
             notification.setTitle(title);
             notification.setMessage(message);
             notification.setType(type);
             notification.setRead(false);
             notifications.add(notification);
        }
        this.notificationRepository.saveAll(notifications);
        
        // Broadcast via topic for real-time receipt by all online users
        this.messagingTemplate.convertAndSend("/topic/notifications", title + ": " + message);
    }
    
    public long countUnread() {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userService.findByUsername(email);
        if (currentUser != null) {
            return this.notificationRepository.countByUserAndIsReadFalse(currentUser);
        }
        return 0;
    }
}
