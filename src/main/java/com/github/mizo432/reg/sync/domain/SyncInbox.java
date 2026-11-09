package com.github.mizo432.reg.sync.domain;

import com.github.mizo432.reg.exception.ValidationException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_inbox")
public class SyncInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_id", nullable = false, length = 50)
    private String eventId;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Column(nullable = false)
    private boolean applied = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new ValidationException("eventType", "イベントタイプは空であってはいけません");
        }
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new ValidationException("eventId", "イベントIDは空であってはいけません");
        }
        this.eventId = eventId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new ValidationException("payload", "ペイロードは空であってはいけません");
        }
        this.payload = payload;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
