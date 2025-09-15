package com.jooyeon.app.repository;

import com.jooyeon.app.domain.entity.member.Member;
import com.jooyeon.app.domain.entity.member.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUserId(String userId);
    Optional<Member> findByUserId(String userId);

    /**
     * 탈퇴 신청한지 30일 이상 지난 회원들 조회
     * @param cutoffDate 30일 전 날짜
     * @return 하드 삭제 대상 회원 목록
     */
    @Query("SELECT m FROM Member m WHERE m.memberStatus = :status AND m.withdrawnAt IS NOT NULL AND m.withdrawnAt <= :cutoffDate")
    List<Member> findMembersWithdrawnBefore(@Param("status") MemberStatus status, @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 탈퇴 신청한지 30일 이상 지난 회원들의 수 조회 (로깅용)
     * @param cutoffDate 30일 전 날짜
     * @return 하드 삭제 대상 회원 수
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.memberStatus = :status AND m.withdrawnAt IS NOT NULL AND m.withdrawnAt <= :cutoffDate")
    long countMembersWithdrawnBefore(@Param("status") MemberStatus status, @Param("cutoffDate") LocalDateTime cutoffDate);

}