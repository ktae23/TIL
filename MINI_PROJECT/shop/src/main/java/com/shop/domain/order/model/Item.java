package com.shop.domain.order.model;

import com.shop.application.order.dto.ItemFormDto;
import com.shop.domain.BaseEntity;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import com.shop.infrastructure.exception.OutOfStockException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

    public void removeStock(int stockNumber) {
        int reStock = this.stockNumber - stockNumber;
        if (reStock < 0) {
            throw new OutOfStockException("상품의 재고가 부족합니다.");
        }
        this.stockNumber = reStock;
    }

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

    public void updateItem(ItemFormDto itemFormDto) {
        this.itemName = itemFormDto.getItemName();
        this.price = itemFormDto.getPrice();
        this.stockNumber = itemFormDto.getStockNumber();
        this.itemDetail = itemFormDto.getItemDetail();
        this.itemSellStatus = itemFormDto.getItemSellStatus();
    }

    public void addStock(int stockNumber) {
        this.stockNumber += stockNumber;
    }

}
