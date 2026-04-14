package com.tuna.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.UserBehavior;

import java.util.List;

@Repository
public interface UserBehaviorRepository
        extends JpaRepository<UserBehavior, Long>, JpaSpecificationExecutor<UserBehavior> {

    List<UserBehavior> findTop10ByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<UserBehavior> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    long countByActionType(com.tuna.ecommerce.ultil.constant.ActionTypeEnum actionType);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT b.sessionId) FROM UserBehavior b")
    long countDistinctSessions();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT b.userEmail) FROM UserBehavior b")
    long countDistinctUsers();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT b.sessionId) FROM UserBehavior b WHERE b.actionType = :actionType")
    long countDistinctSessionsByActionType(@org.springframework.data.repository.query.Param("actionType") com.tuna.ecommerce.ultil.constant.ActionTypeEnum actionType);

    @org.springframework.data.jpa.repository.Query("SELECT b.actionType, COUNT(b) FROM UserBehavior b GROUP BY b.actionType")
    List<Object[]> findActionDistribution();

    @org.springframework.data.jpa.repository.Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM user_behaviors WHERE created_at > :startDate GROUP BY date ORDER BY date ASC", nativeQuery = true)
    List<Object[]> findActivityTrend(@org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate);
}
