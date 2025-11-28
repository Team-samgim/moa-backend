/**
 * 작성자: 정소영
 */
package com.moa.api.member.service;

import com.moa.api.member.dto.MemberDTO;
import com.moa.api.member.entity.Member;
import com.moa.api.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberDTO getMyInfo(Long userId) {
        Member m = memberRepository.findById(userId).orElseThrow();
        return new MemberDTO(m.getId(), m.getLoginId(), m.getEmail(), m.getNickname(), m.getRole().name());
    }
}