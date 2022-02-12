package jpql;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
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

            Member member = new Member();
            member.setUsername("TeamA");
            member.setAge(10);
            member.setTeam(team);

            em.persist(member);

            em.flush();
            em.clear();

//              세타 조인 ( cross join)
//            String query = "select m from Member m, Team t where m.username = t.name";

//            연관관계 없는 엔티티 조인
//            String query = "select m from Member m left join Team t on t.name = m.username";

            String query = "select m from Member m left join m.team t on t.name = 'TeamA'";
            List<Member> resultList = em.createQuery(query, Member.class)
                    .getResultList();

            System.out.println("resultList = " + resultList.size());


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
