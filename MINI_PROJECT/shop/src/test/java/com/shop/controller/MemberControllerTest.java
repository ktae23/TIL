package com.shop.controller;

import com.shop.dto.MemberFormDto;
import com.shop.entity.Member;
import com.shop.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

@SpringBootTest(args = "--spring.profiles.active=test")
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Member createMember(String email, String password) {
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .email(email)
                .password(password)
                .name("홍길동")
                .address("서울시 마포구 합정동")
                .build();
        Member member = Member.from(memberFormDto, passwordEncoder);
        return memberService.saveMember(member);
    }



    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest () throws Exception {
        // given
        String email = "test@email.com";
        String password = "12341234";
        this.createMember(email, password);
        // when
        ResultActions resultActions = mockMvc.perform(formLogin()
                .userParameter("email")
                .loginProcessingUrl("/members/login")
                .user(email)
                .password(password));
        // then
        resultActions.andExpect(SecurityMockMvcResultMatchers.authenticated());
    }

    @Test
    @DisplayName("로그인 실패 테스트")
    void loginFailTest () throws Exception {
        // given
        String email = "test@email.com";
        String password = "12341234";
        this.createMember(email, password);
        // when
        ResultActions resultActions = mockMvc.perform(formLogin()
                .userParameter("email")
                .loginProcessingUrl("/members/login")
                .user(email)
                .password("12341235"));
        // then
        resultActions.andExpect(SecurityMockMvcResultMatchers.unauthenticated());
    }
}