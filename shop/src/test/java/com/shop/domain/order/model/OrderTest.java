package com.shop.domain.order.model;

import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.repository.ItemRepository;
import com.shop.domain.order.repository.OrderRepository;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(args = "--spring.profiles.active=test")
@Transactional
class OrderTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    MemberRepository memberRepository;

    @PersistenceContext
    EntityManager em;

    Item createItem() {
        return Item.builder()
                .itemName("테스트 상품")
                .price(10000)
                .itemDetail("상세설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .stockNumber(100)
                .regTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("영속성 전이 테스트")
    void cascadeTest() throws Exception {
        // given
        Order order = new Order();

        for (int i = 0; i < 3; i++) {
            Item item = createItem();
            itemRepository.save(item);
            OrderItem orderItem = OrderItem.builder()
                    .item(item)
                    .count(10)
                    .orderPrice(1000)
                    .order(order)
                    .build();
            orderItem.addOrder(order);
        }

        // when
        orderRepository.saveAndFlush(order);
        em.clear();
        // then
        Order savedOrder = orderRepository.findById(order.getId())
                .orElseThrow(EntityNotFoundException::new);
        assertThat(savedOrder.getOrderItems().size()).isEqualTo(3);
    }


}