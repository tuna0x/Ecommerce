package com.tuna.ecommerce.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.ContactMessage;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.ContactMessageService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class ContactController {
    private final ContactMessageService contactMessageService;

    @PostMapping("/public/contact")
    @APIMessage("Gửi liên hệ thành công! Chúng tôi sẽ phản hồi sớm nhất.")
    public ResponseEntity<ContactMessage> createContact(@Valid @RequestBody ContactMessage contactMessage) {
        ContactMessage res = this.contactMessageService.handleCreate(contactMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/contact")
    @APIMessage("Lấy danh sách liên hệ thành công")
    public ResponseEntity<ResultPaginationDTO> getMessages(
            @Filter Specification<ContactMessage> spec,
            Pageable pageable) {
        return ResponseEntity.ok().body(this.contactMessageService.handleGetMessages(pageable));
    }

    @PatchMapping("/contact/{id}/status")
    @APIMessage("Cập nhật trạng thái liên hệ thành công")
    public ResponseEntity<ContactMessage> updateStatus(@PathVariable long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok().body(this.contactMessageService.handleUpdateStatus(id, status));
    }

    @DeleteMapping("/contact/{id}")
    @APIMessage("Xóa liên hệ thành công")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        this.contactMessageService.handleDelete(id);
        return ResponseEntity.ok().body(null);
    }
}
