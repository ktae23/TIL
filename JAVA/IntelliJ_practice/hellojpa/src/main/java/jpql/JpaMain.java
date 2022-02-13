package jpql;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;

public class JpaMain {

    public static void main(String[] args) {
        // 쓰레드간 공유하면 안됨, 사용하고 바로 버려야 함
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {

            Team team = new Team();
            team.setName("TeamA");
            em.persist(team);

            Team team2 = new Team();
            team2.setName("TeamB");
            em.persist(team2);

            Member member = new Member();
            member.setUsername("회원1");
            member.setTeam(team);

            Member member2 = new Member();
            member2.setUsername("회원2");
            member2.setTeam(team);

            Member member3 = new Member();
            member3.setUsername("회원3");
            member3.setTeam(team2);

            em.persist(member);
            em.persist(member2);
            em.persist(member3);

            // flush 자동 호출
            // 영속성 컨텐스트 반영하기 전에 벌크 연산을 먼저 실행
            // 또는 벌크 연산 이후 영속성 컨텍스트 초기화
            // DB에 반영했는데 영속성 컨텍스트에는 이전 값이 남아 있기 때문
            int resultCount = em.createQuery("update Member m set m.age = 20")
                    .executeUpdate();
            System.out.println("resultCount = " + resultCount);
            em.clear();

            Member findMember = em.find(Member.class, member.getId());
            System.out.println("findMember.getAge() = " + findMember.getAge());
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
        emf.close();

    }
}
