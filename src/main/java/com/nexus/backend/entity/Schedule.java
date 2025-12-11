package com.nexus.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "all_day")
    @Builder.Default
    private Boolean allDay = false;

    @Column(length = 20)
    private String color;

    @Column(length = 50)
    private String location;

    // Outlook Calendar 연동 필드
    @Column(name = "outlook_event_id", unique = true)
    private String outlookEventId;

    @Column(name = "is_from_outlook")
    @Builder.Default
    private Boolean isFromOutlook = false;

    // 참석자 정보 (Outlook에서 가져온 경우)
    @Column(name = "attendees", columnDefinition = "TEXT")
    private String attendees;

    // 주최자 정보 (Outlook에서 가져온 경우)
    @Column(name = "organizer")
    private String organizer;

    // Many-to-One relationship with Project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // Many-to-Many relationship with ScheduleCategory
    @ManyToMany
    @JoinTable(
        name = "schedule_category_mappings",
        joinColumns = @JoinColumn(name = "schedule_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private List<ScheduleCategory> categories = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
