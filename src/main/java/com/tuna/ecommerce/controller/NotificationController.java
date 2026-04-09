package com.tuna.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Notification;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.request.notification.ReqSendNotificationDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.NotificationService;
import com.tuna.ecommerce.service.UserService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @APIMessage("Get all notifications for current user")
    @GetMapping("/notifications")
    public ResponseEntity<ResultPaginationDTO> getNotifications(Pageable pageable) {
        return ResponseEntity.ok().body(this.notificationService.getNotificationsByUser(pageable));
    }

    @APIMessage("Mark notification as read")
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable("id") long id) throws IdInvalidException {
        Notification notification = this.notificationService.markAsRead(id);
        if (notification == null) {
            throw new IdInvalidException("Notification not found with id: " + id);
        }
        return ResponseEntity.ok().body(notification);
    }

    @APIMessage("Mark all notifications as read")
    @PutMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        this.notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
    
    @APIMessage("Count unread notifications")
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Long> countUnread() {
        return ResponseEntity.ok().body(this.notificationService.countUnread());
    }

    @APIMessage("Admin sends notification to a user")
    @org.springframework.web.bind.annotation.PostMapping("/notifications/send")
    public ResponseEntity<Notification> sendNotification(@org.springframework.web.bind.annotation.RequestBody ReqSendNotificationDTO req) throws IdInvalidException {
        if (req.getUserId() == null) {
            throw new IdInvalidException("User ID must not be null");
        }
        User user = this.userService.getUserById(req.getUserId());
        if (user == null) {
            throw new IdInvalidException("User not found with id: " + req.getUserId());
        }
        Notification notification = this.notificationService.createNotification(user, req.getTitle(), req.getMessage(), req.getType());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(notification);
    }
}
