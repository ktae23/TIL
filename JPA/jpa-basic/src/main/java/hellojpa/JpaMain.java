package hellojpa;

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
//            // 비영속 상태
//            Member member1 = new Member(3L, "HelloC");
//            Member member2 = new Member(4L, "HelloD");

            Member member = em.find(Member.class, 4L);
            System.out.println("member = " + member);
//            member.setName("ZZZZ");
            // 영속 시작
            em.persist(member);

//            Member findMember1 = em.find(Member.class, 2L);
//            Member findMember2 = em.find(Member.class, 2L);
//            System.out.println("findMember1.toString() = " + findMember1.toString());
//            System.out.println("findMember2.toString() = " + findMember2.toString());
//
//            System.out.println(findMember1 == findMember2);

//            Member findMember = em.find(Member.class, 1L);

//            em.remove(findMember);
            // 한 트랜잭션 안에서는 관리가 되기 때문에 변경된 사항이 발생하면 업데이트 쿼리가 발생함
            // 모든 데이터 변경은 트랜잭션 안에서 실행해야 함
//            findMember.setName("HelloJPA");

//            List<Member> result = em.createQuery("select m from Member as m", Member.class)
//                    .setFirstResult(5)
//                    .setMaxResults(10)
//                    .getResultList();
//
//            for (Member member : result) {
//                System.out.println("member.getName() = " + member.getName());
//            }

            // 쿼리 실행
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
        emf.close();

    }
}
