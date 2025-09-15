package com.jooyeon.app.repository;

import com.jooyeon.app.domain.entity.common.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<IdempotencyRecord> findByIdempotencyKeyAndResourceType(String idempotencyKey, String resourceType);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByIdempotencyKey(String idempotencyKey);
}