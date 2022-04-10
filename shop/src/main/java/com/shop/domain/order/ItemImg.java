package com.shop.domain.order;

import com.shop.domain.BaseEntity;
import com.shop.domain.order.model.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ItemImg extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String imgName;
    private String originImgName;
    private String imgUrl;
    private Boolean representImgYn;

    @ManyToOne(fetch = FetchType.LAZY)
    private Item item;

    public void updateItemImg(String originImgName, String imgName, String imgUrl) {
        this.originImgName = originImgName;
        this.imgName = imgName;
        this.imgUrl = imgUrl;
    }
}
