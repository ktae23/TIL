package com.shop.dto;

import com.shop.entity.Item;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ItemDto {
    private Long id;
    private String itemName;
    private Integer price;
    private String itemDetail;
    private String sellStatusCode;
    private LocalDateTime regTime;
    private LocalDateTime updateTime;

    public static ItemDto of(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .price(item.getPrice())
                .itemDetail(item.getItemDetail())
                .sellStatusCode(item.getItemSellStatus().name())
                .regTime(item.getRegTime())
                .updateTime(item.getUpdateTime())
                .build();
    }
}
