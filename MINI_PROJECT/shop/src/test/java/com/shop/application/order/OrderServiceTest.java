package com.shop.application.order;

import com.shop.application.order.dto.OrderDto;
import com.shop.domain.member.model.Member;
import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.model.Order;
import com.shop.domain.order.model.OrderItem;
import com.shop.domain.order.repository.ItemRepository;
import com.shop.domain.order.repository.OrderRepository;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import com.shop.infrastructure.constant.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(args = "--spring.profiles.active=test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private MemberRepository memberRepository;

    public Item saveItem() {
        Item item = Item.builder()
                .itemName("테스트 상품")
                .price(10000)
                .itemDetail("테스트 상품 상세 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .stockNumber(100)
                .build();

        return itemRepository.save(item);
    }

    public Member saveMember() {
        Member member = Member.builder()
                .email("test@test.com")
                .build();
        return memberRepository.save(member);
    }
    @Test
    @DisplayName("주문 테스트")
    void order () throws Exception {
        // given
        final Item item = saveItem();
        final Member member = saveMember();
        final OrderDto orderDto = OrderDto.builder().count(10).id(item.getId()).build();

        // when
        final Long orderId = orderService.order(orderDto, member.getEmail());
        final Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        final List<OrderItem> orderItems = order.getOrderItems();
        final int totalPrice = orderDto.getCount() * item.getPrice();

        // then
        assertThat(totalPrice).isEqualTo(order.getTotalPrice());
    }

    @Test
    @DisplayName("주문 취소 테스트")
    void cancelOrder () throws Exception {
        // given
        final Item item = saveItem();
        final Member member = saveMember();
        final OrderDto orderDto = OrderDto.builder().id(item.getId()).count(10).build();
        // when
        final Long orderId = orderService.order(orderDto, member.getEmail());
        final Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        orderService.cancelOrder(orderId);
        // then
        assertThat(OrderStatus.CANCEL).isEqualTo(order.getOrderStatus());
        assertThat(100).isEqualTo(item.getStockNumber());
    }

}