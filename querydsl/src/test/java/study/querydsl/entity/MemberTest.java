package study.querydsl.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    @Test
    public void testEntity() throws Exception {
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);


        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);


        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        em.flush();
        em.clear();

        // when


        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member2 = " + member);
        }


        // then

        assertThat(members.size()).isEqualTo(4);
        assertThat(members.stream().filter(m -> m.getAge() > 20).collect(Collectors.toList()).size()).isEqualTo(2);
        assertThat(members.stream().filter(m -> m.getTeam().getName() == "teamA").collect(Collectors.toList()).size()).isEqualTo(2);
        assertThat(members.stream().filter(m -> m.getTeam().getName() == "teamB").collect(Collectors.toList()).size()).isEqualTo(2);

    }
}