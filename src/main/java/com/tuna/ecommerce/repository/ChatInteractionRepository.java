package com.tuna.ecommerce.repository;

import com.tuna.ecommerce.domain.ChatInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatInteractionRepository extends JpaRepository<ChatInteraction, Long> {
    List<ChatInteraction> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<ChatInteraction> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
