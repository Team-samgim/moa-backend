/**
 * 작성자: 정소영
 */
package com.moa.api.member.repository;

import com.moa.api.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);      // 이메일로 조회
    boolean existsByEmail(String email);             // 이메일 중복확인

    Optional<Member> findByLoginId(String loginId);  // 로그인ID로 조회
    boolean existsByLoginId(String loginId);         // 로그인ID 중복확인
}