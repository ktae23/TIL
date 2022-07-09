package com.shop.application.order.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@Builder
@ToString
public class CartDetailDto {

    private Long cartItemId;
    private String itemName;
    private int price;
    private int count;
    private String imgUrl;


    @QueryProjection
    public CartDetailDto(Long cartItemId, String itemName, int price, int count, String imgUrl) {
        this.cartItemId = cartItemId;
        this.itemName = itemName;
        this.price = price;
        this.count = count;
        this.imgUrl = imgUrl;
    }
}
