package com.shop.application.order;

import com.shop.application.order.dto.OrderDto;
import com.shop.application.order.dto.OrderHistoryDto;
import com.shop.application.order.dto.OrderItemDto;
import com.shop.domain.member.model.Member;
import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.ItemImg;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.model.Order;
import com.shop.domain.order.model.OrderItem;
import com.shop.domain.order.repository.ItemImgRepository;
import com.shop.domain.order.repository.ItemRepository;
import com.shop.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final ItemImgRepository itemImgRepository;

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

    @Transactional(readOnly = true)
    public Page<OrderHistoryDto> getOrderList(String email, Pageable pageable) {

        final List<Order> orders = orderRepository.findOrders(email, pageable);
        final Long totalCount = orderRepository.countOrder(email);

        List<OrderHistoryDto> orderHistoryDtoList = orders.stream()
                .map(order -> {

                    final OrderHistoryDto historyDto = OrderHistoryDto.of(order);
                    final List<OrderItem> orderItems = order.getOrderItems();

                    final List<OrderItemDto> orderItemDtoList = orderItems.stream()
                            .map(orderItem -> {

                                final ItemImg representImg = itemImgRepository.findByItemIdAndRepresentImgYn(orderItem.getItem().getId(), TRUE);

                                return new OrderItemDto(orderItem, representImg.getImgUrl());

                            }).collect(Collectors.toList());

                    historyDto.addAllOrderItemDto(orderItemDtoList);

                    return historyDto;

                }).collect(Collectors.toList());

        return new PageImpl<>(orderHistoryDtoList, pageable, totalCount);
    }

    @Transactional(readOnly = true)
    public boolean validateOrder(Long orderId, String email) {
        final Member currentMember = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        final Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        final Member orderMember = order.getMember();

        return orderMember.getEmail().equals(currentMember.getEmail());
    }

    public void cancelOrder(Long orderId) {
        final Order order = orderRepository.findById(orderId).orElseThrow(EntityNotFoundException::new);
        order.cancelOrder();
    }

    public Long orders(List<OrderDto> orderDtoList, String email) {
        final Member member = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        List<OrderItem> orderItemList = orderDtoList.stream()
                .map(order -> {
                    final Item item = itemRepository.findById(order.getId()).orElseThrow(EntityNotFoundException::new);
                    return OrderItem.createOrderItem(item, order.getCount());
                })
                .collect(Collectors.toList());
        final Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order);
        return order.getId();
    }
}
