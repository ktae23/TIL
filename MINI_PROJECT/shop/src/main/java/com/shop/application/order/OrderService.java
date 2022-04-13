package com.shop.application.order;

import com.shop.application.order.dto.OrderDto;
import com.shop.domain.member.model.Member;
import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.model.Order;
import com.shop.domain.order.model.OrderItem;
import com.shop.domain.order.repository.ItemRepository;
import com.shop.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public Long order(OrderDto orderDto, String email) {
        final Item item = itemRepository.findById(orderDto.getId()).orElseThrow(EntityNotFoundException::new);
        final Member member = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        final ArrayList<OrderItem> orderItemList = new ArrayList<>();

        final OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
        orderItemList.add(orderItem);

        final Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order);
        return order.getId();

    }

}
