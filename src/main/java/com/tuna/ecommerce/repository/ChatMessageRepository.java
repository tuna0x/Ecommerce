package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tuna.ecommerce.domain.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderEmail = :user1 AND m.receiverEmail = :user2) OR " +
           "(m.senderEmail = :user2 AND m.receiverEmail = :user1) " +
           "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findChatHistory(@Param("user1") String user1, @Param("user2") String user2, Pageable pageable);

    @Query(value = "SELECT * FROM chat_messages WHERE id IN (" +
            "SELECT MAX(id) FROM chat_messages " +
            "WHERE sender_email = :email OR receiver_email = :email " +
            "GROUP BY CASE WHEN sender_email = :email THEN receiver_email ELSE sender_email END" +
            ") ORDER BY timestamp DESC", nativeQuery = true)
    List<ChatMessage> findRecentConversations(@Param("email") String email);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.senderEmail = :sender AND m.receiverEmail = :receiver AND m.isRead = false")
    long countUnreadMessages(@Param("sender") String sender, @Param("receiver") String receiver);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderEmail = :sender AND m.receiverEmail = :receiver AND m.isRead = false")
    void markMessagesAsRead(@Param("sender") String sender, @Param("receiver") String receiver);
}
