package com.shop.entity;

import com.shop.constant.ItemSellStatus;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 50)
    private String itemNumber;

    @Column(nullable = false)
    private int price;
    @Column(nullable = false)
    private int stockNumber;
    @Lob
    @Column(nullable = false)
    private String itemDetail;
    @Enumerated(EnumType.STRING)
    private ItemSellStatus itemSellStatus;

    private LocalDateTime regTime;
    private LocalDateTime updateTime;

}
