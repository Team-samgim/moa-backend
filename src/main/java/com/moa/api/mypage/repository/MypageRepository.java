package com.moa.api.mypage.repository;

import com.moa.api.preset.entity.Preset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MypageRepository extends JpaRepository<Preset, Integer> {

    @Query("SELECT COUNT(p) FROM Preset p WHERE p.member.id = :memberId AND p.origin = 'USER'")
    long countByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(p) FROM Preset p WHERE p.member.id = :memberId AND p.favorite = true")
    long countFavoriteByMemberId(@Param("memberId") Long memberId);
}