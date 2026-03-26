package com.tuna.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderEmail = :user1 AND m.receiverEmail = :user2) OR " +
           "(m.senderEmail = :user2 AND m.receiverEmail = :user1) " +
           "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findChatHistory(@Param("user1") String user1, @Param("user2") String user2, Pageable pageable);
}
