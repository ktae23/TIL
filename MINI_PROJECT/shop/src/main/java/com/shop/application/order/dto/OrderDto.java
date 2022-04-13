package com.shop.application.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class OrderDto {

    @NotNull(message = "상품 아이디는 필수 입력값입니다.")
    private Long id;

    @Min(value = 1, message = "최소 주문 수량은 1개입니다.")
    @Max(value = 999, message = "최대 주문 수량은 999개입니다.")
    private int count;
}
