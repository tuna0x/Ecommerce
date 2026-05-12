package com.tuna.ecommerce.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skincare_checkins")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SkincareCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User user;

    private int streak;

    private LocalDate lastCheckIn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "skincare_checkin_history", joinColumns = @JoinColumn(name = "checkin_id"))
    @Column(name = "checkin_date")
    private List<String> history = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "skincare_checkin_milestones", joinColumns = @JoinColumn(name = "checkin_id"))
    @Column(name = "milestone_id")
    private List<String> claimedMilestones = new ArrayList<>();
}
