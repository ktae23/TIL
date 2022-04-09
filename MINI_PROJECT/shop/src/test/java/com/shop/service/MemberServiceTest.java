package com.shop.service;

import com.shop.dto.MemberFormDto;
import com.shop.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest(args = "--spring.profiles.active=test")
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    PasswordEncoder passwordEncoder;

    Member createMember() {
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .email("test@email.com")
                .name("홍길동")
                .address("서울시 마포구 합정동")
                .password("1234")
                .build();
        return Member.from(memberFormDto, passwordEncoder);
    }

    @Test
    @DisplayName("회원가입 테스트")
    void saveMemberTest() throws Exception {
        // given
        Member member = createMember();
        // when
        Member saveMember = memberService.saveMember(member);

        // then
        assertThat(member.getAddress()).isEqualTo(saveMember.getAddress());
        assertThat(member.getEmail()).isEqualTo(saveMember.getEmail());
        assertThat(member.getName()).isEqualTo(saveMember.getName());
        assertThat(member.getPassword()).isEqualTo(saveMember.getPassword());
        assertThat(member.getRole()).isEqualTo(saveMember.getRole());
    }

    @Test
    @DisplayName("중복 회원 가입 테스트")
    void saveDuplicateMemberTest() throws Exception {
        // given
        Member member1 = createMember();
        Member member2 = createMember();
        // when
        memberService.saveMember(member1);

        // then
        assertThatThrownBy(() -> {
            memberService.saveMember(member2);
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 가입된 회원입니다.");
    }
}