package com.jooyeon.app.domain.entity.common;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records",
       uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"),
       indexes = {
           @Index(name = "idx_idempotency_key", columnList = "idempotency_key"),
           @Index(name = "idx_expires_at", columnList = "expires_at"),
           @Index(name = "idx_resource_type_key", columnList = "resource_type, idempotency_key")
       })
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String resourceId, String resourceType,
                           LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}