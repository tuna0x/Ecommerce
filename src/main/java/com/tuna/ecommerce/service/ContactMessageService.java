package com.tuna.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tuna.ecommerce.domain.ContactMessage;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.repository.ContactMessageRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ContactMessageService {
    private final ContactMessageRepository contactMessageRepository;
    private final TelegramService telegramService;

    public ContactMessage handleCreate(ContactMessage message) {
        ContactMessage savedMessage = this.contactMessageRepository.save(message);
        
        // Notify Admin via Telegram
        this.notifyAdmin(savedMessage);
        
        return savedMessage;
    }

    public ResultPaginationDTO handleGetMessages(Pageable pageable) {
        Page<ContactMessage> page = this.contactMessageRepository.findAll(pageable);
        ResultPaginationDTO res = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());

        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        res.setMeta(meta);
        res.setResult(page.getContent());
        return res;
    }

    public ContactMessage handleUpdateStatus(long id, String status) {
        ContactMessage msg = this.contactMessageRepository.findById(id).orElse(null);
        if (msg != null) {
            msg.setStatus(status);
            return this.contactMessageRepository.save(msg);
        }
        return null;
    }

    public void handleDelete(long id) {
        this.contactMessageRepository.deleteById(id);
    }

    private void notifyAdmin(ContactMessage msg) {
        String telegramMsg = "<b>📬 CÓ TIN NHẮN LIÊN HỆ MỚI!</b>\n\n" +
                "<b>👤 Khách hàng:</b> " + msg.getName() + "\n" +
                "<b>📧 Email:</b> " + msg.getEmail() + "\n" +
                "<b>📞 SĐT:</b> " + (msg.getPhone() != null ? msg.getPhone() : "N/A") + "\n" +
                "<b>📌 Chủ đề:</b> " + msg.getSubject() + "\n" +
                "<b>💬 Nội dung:</b> <i>\"" + msg.getMessage() + "\"</i>\n\n" +
                "👉 Admin hãy kiểm tra và phản hồi khách hàng nhé!";
        
        this.telegramService.sendAiAlert(msg.getEmail(), msg.getMessage(), "KHÁCH HÀNG LIÊN HỆ QUA FORM");
    }
}
