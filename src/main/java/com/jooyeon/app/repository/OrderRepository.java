package com.jooyeon.app.repository;

import com.jooyeon.app.domain.entity.order.Order;
import com.jooyeon.app.domain.entity.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdAndMemberId(Long id, Long memberId);
}