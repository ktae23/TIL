package com.shop.domain.order.model;

import com.shop.application.order.dto.ItemFormDto;
import com.shop.domain.BaseEntity;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@ToString
@EqualsAndHashCode
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 50)
    private String itemName;

    @Column(nullable = false)
    private int price;
    @Column(nullable = false)
    private int stockNumber;
    @Lob
    @Column(nullable = false)
    private String itemDetail;
    @Enumerated(EnumType.STRING)
    private ItemSellStatus itemSellStatus;

    public static Item from(ItemFormDto itemFormDto) {
        return Item.builder()
                .id(itemFormDto.getId())
                .itemName(itemFormDto.getItemName())
                .price(itemFormDto.getPrice())
                .itemDetail(itemFormDto.getItemDetail())
                .stockNumber(itemFormDto.getStockNumber())
                .itemSellStatus(itemFormDto.getItemSellStatus())
                .build();

    }

}
