package com.beyond.MKX.domain.member.service;

import com.beyond.MKX.domain.member.domain.Member;
import com.beyond.MKX.domain.member.dto.LoginDto;
import com.beyond.MKX.domain.member.dto.MemberSaveReqDto;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long save(MemberSaveReqDto memberSaveReqDto){
        Optional<Member> optionalMember =  memberRepository.findByEmail(memberSaveReqDto.getEmail());
        if(optionalMember.isPresent()){
            throw new IllegalArgumentException("기존에 존재하는 회원입니다.");
        }
        String password = passwordEncoder.encode(memberSaveReqDto.getPassword());
        Member member = memberRepository.save(memberSaveReqDto.toEntity(password));
        return  member.getId();
    }
    public Member login(LoginDto dto){
        boolean check = true;
//        email존재여부
        Optional<Member> optionalMember = memberRepository.findByEmail(dto.getEmail());
        if(!optionalMember.isPresent()){
            check = false;
        }
//        password일치 여부
        if(!passwordEncoder.matches(dto.getPassword(), optionalMember.get().getPassword())){
            check =false;
        }
        if(!check){
            throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
        }
        return optionalMember.get();
    }
}
