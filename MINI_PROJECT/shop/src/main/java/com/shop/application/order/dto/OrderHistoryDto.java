package com.shop.application.order.dto;

import com.shop.domain.order.model.Order;
import com.shop.infrastructure.constant.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class OrderHistoryDto {

    private Long orderId;
    private String orderDate;
    private OrderStatus orderStatus;
    private List<OrderItemDto> orderItemDtoList = new ArrayList<>();

    public static OrderHistoryDto of(Order order) {
        return new OrderHistoryDto(order.getId(), order.getOrderDate().format(DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm")), order.getOrderStatus(), new ArrayList<>());
    }

    public void addOrderItemDto(OrderItemDto orderItemDto) {
        this.orderItemDtoList.add(orderItemDto);
    }
    public void addAllOrderItemDto(List<OrderItemDto> orderItemDto) {
        this.orderItemDtoList.addAll(orderItemDto);
    }
}
