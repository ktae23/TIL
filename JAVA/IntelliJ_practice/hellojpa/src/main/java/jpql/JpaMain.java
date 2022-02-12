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
            Member member = new Member();
            member.setUsername("member1");
            member.setAge(10);

            em.persist(member);

            em.flush();
            em.clear();

//            List<Member> members = em.createQuery("select m.team from Member m", Member.class)
//                    .getResultList();
//
//            // join이 나간다고 예측하기 좋도록 명시하는것 추천
//            List<Team> team = em.createQuery("select m.team from Member m join m.team t", Team.class)
//                    .getResultList();
//
//            List<Address> resultList = em.createQuery("select o.address from Order o", Address.class)
//                    .getResultList();


//            // 스칼라 타입 조회 1 (Object 배열 반환)
//            List resultList1 = em.createQuery("select distinct m.username, m.age from Member m")
//                    .getResultList();
//            Object o = resultList1.get(0);
//            Object[] result = (Object[]) o;
//            System.out.println("username = " + result[0]);
//            System.out.println("age = " + result[1]);

//            // 스칼라 타입 조회 2 (제네릭에 Object 배열 선언)
//            List<Object[]> resultList2 = em.createQuery("select distinct m.username, m.age from Member m")
//                    .getResultList();
//            Object[] result = resultList2.get(0);
//            System.out.println("username = " + result[0]);
//            System.out.println("age = " + result[1]);

            // 스칼라 타입 조회 3 (dto로 바로 조회)
            List<MemberDTO> result = em.createQuery("select new jpql.MemberDTO(m.username, m.age) from Member m", MemberDTO.class)
                    .getResultList();
            MemberDTO memberDTO = result.get(0);
            System.out.println("username = " + memberDTO.getUsername());
            System.out.println("age = " + memberDTO.getAge());



//            Member member1 = em.createQuery("select m from Member m where m.username = :username", Member.class)
//                    .setParameter("username", "member1")
//                    .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
        } finally {
            em.close();
        }
        emf.close();

    }
}
