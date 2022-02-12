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

            em.flush();
            em.clear();

            // 중복 값으로 인한 조회값 추가 조회 문제
            // SQL distinct는 모든 값이 같아야 되기 때문에 애플리케이션 수준에서 같은 엔티티일 경우 중복 제거를 해줘야 한다.
            // JPQL의 distinct는 이 기능을 제공
            String query = "select distinct t from Team t join fetch t.members";
            List<Team> result = em.createQuery(query, Team.class)
                    .getResultList();

            for (Team teams : result) {
                System.out.println("teams  = " + teams.getName() + " | members.size() = " + teams.getMembers().size()) ;
                for (Member members : teams.getMembers()) {
                    System.out.println("member = " + members);
                }
            }

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
