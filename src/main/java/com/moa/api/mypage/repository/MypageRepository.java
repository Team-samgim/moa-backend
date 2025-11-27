package com.moa.api.mypage.repository;

import com.moa.api.preset.entity.Preset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/*****************************************************************************
 CLASS NAME    : MypageRepository
 DESCRIPTION   : 마이페이지 통계 조회용 Repository
 - 회원별 프리셋 수, 즐겨찾기 수 등을 계산하기 위한 쿼리 제공
 AUTHOR        : 방대혁
 ******************************************************************************/
public interface MypageRepository extends JpaRepository<Preset, Integer> {

    /**
     * 해당 유저가 생성한 USER 기원 프리셋 개수 조회
     */
    @Query("SELECT COUNT(p) FROM Preset p WHERE p.member.id = :memberId AND p.origin = 'USER'")
    long countByMemberId(@Param("memberId") Long memberId);

    /**
     * 해당 유저가 즐겨찾기한 프리셋 개수 조회
     */
    @Query("SELECT COUNT(p) FROM Preset p WHERE p.member.id = :memberId AND p.favorite = true")
    long countFavoriteByMemberId(@Param("memberId") Long memberId);
}
