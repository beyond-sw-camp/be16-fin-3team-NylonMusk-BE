package com.beyond.MKX.member.service;

import com.beyond.MKX.member.domain.Member;
import com.beyond.MKX.member.domain.Role;
import com.beyond.MKX.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

//CommandLineRunner를 상속함으로서 해당 컴포넌트가 스프링빈으로 등록되는 시점에서 run메서드 자동실행
@Component
public class InitialDataLoader implements CommandLineRunner {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    public InitialDataLoader(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public void run(String... args) throws Exception {
        if(memberRepository.findByEmail("admin@naver.com").isPresent())return;
        Member member = Member.builder()
                .name("admin")
                .email("admin@naver.com")
                .password(passwordEncoder.encode("12341234"))
                .role(Role.ADMIN)
                .build();
        memberRepository.save(member);
    }
}
