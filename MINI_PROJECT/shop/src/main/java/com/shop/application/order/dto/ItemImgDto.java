package com.shop.application.order.dto;

import com.shop.domain.order.ItemImg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ItemImgDto {

    private Long id;
    private String imgName;
    private String originImgName;
    private String imgUrl;
    private Boolean representImgYn;

    public static ItemImgDto of(ItemImg itemImg) {
        return ItemImgDto.builder()
                .id(itemImg.getId())
                .imgName(itemImg.getImgName())
                .originImgName(itemImg.getOriginImgName())
                .imgUrl(itemImg.getImgUrl())
                .representImgYn(itemImg.getRepresentImgYn())
                .build();
    }
}
