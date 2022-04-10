package com.shop.domain.member.model;

import com.shop.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(args = "--spring.profiles.active=test")
@Transactional
class MemberTest {

    @Autowired
    MemberRepository memberRepository;

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("Auditing 테스트")
    @WithMockUser(username = "gildong", authorities = "USER")
    void auditingTest () throws Exception {
        // given
        Member newMember = new Member();
        memberRepository.save(newMember);

        em.flush();
        em.clear();

        // when
        Member member = memberRepository.findById(newMember.getId())
                .orElseThrow(EntityNotFoundException::new);
        // then
        assertThat(member.getCreatedBy()).isNotNull();
        assertThat(member.getModifiedBy()).isNotNull();
        assertThat(member.getCreatedBy()).isEqualTo("gildong");
        assertThat(member.getModifiedBy()).isEqualTo("gildong");
    }
}