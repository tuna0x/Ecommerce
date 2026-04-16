package com.tuna.ecommerce.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tuna.ecommerce.domain.FlashSaleCampaign;

@Repository
public interface FlashSaleCampaignRepository extends JpaRepository<FlashSaleCampaign, Long> {
    
    @Query("SELECT c FROM FlashSaleCampaign c WHERE c.active = true AND c.startAt <= :now AND c.endAt >= :now")
    List<FlashSaleCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM FlashSaleCampaign c WHERE c.active = true AND c.startAt > :now ORDER BY c.startAt ASC")
    List<FlashSaleCampaign> findUpcomingCampaigns(@Param("now") LocalDateTime now);
}
