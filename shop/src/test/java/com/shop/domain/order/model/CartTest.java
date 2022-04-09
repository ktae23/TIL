package com.shop.domain.order.model;

import com.shop.application.member.dto.MemberFormDto;
import com.shop.domain.member.model.Member;
import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.repository.CartRepository;
import com.shop.infrastructure.constant.member.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(args = "--spring.profiles.active=test")
@Transactional
class CartTest {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @PersistenceContext
    EntityManager em;


    public Member createMember() {
        MemberFormDto memberFormDto = MemberFormDto.builder()
                .email("test@email.com")
                .password("12341234")
                .name("홍길동")
                .address("서울시 마포구 합정동")
                .build();
        return Member.from(memberFormDto,  Role.USER, passwordEncoder);
    }

    @Test
    @DisplayName("장바구니 회원 엔티티 매핑 조회 테스트")
    void findCartAndMemberTest () throws Exception {
        // given
        Member member = createMember();
        memberRepository.save(member);

        Cart cart = Cart.builder()
                .member(member)
                .build();
        cartRepository.save(cart);

        em.flush();
        em.clear();
        // when
        Cart savedCart = cartRepository.findById(cart.getId()).orElseThrow(EntityNotFoundException::new);
        // then
        assertThat(savedCart.getMember().getId()).isEqualTo(member.getId());
    }




}