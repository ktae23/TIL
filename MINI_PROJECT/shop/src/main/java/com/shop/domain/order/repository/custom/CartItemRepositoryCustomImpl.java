package com.shop.domain.order.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.application.order.dto.CartDetailDto;
import com.shop.application.order.dto.QCartDetailDto;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.shop.domain.order.QItemImg.itemImg;
import static com.shop.domain.order.model.QCartItem.cartItem;
import static java.lang.Boolean.TRUE;


@RequiredArgsConstructor
public class CartItemRepositoryCustomImpl implements CartItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CartDetailDto> findCartDetailDtoList(Long cartId) {
        return queryFactory
                .select(new QCartDetailDto(
                        cartItem.id,
                        itemImg.imgName,
                        cartItem.item.price,
                        cartItem.count,
                        itemImg.imgUrl
                ))
                .from(cartItem, itemImg)
                .join(cartItem.item)
                .where(
                        cartItem.cart.id.eq(cartId),
                        itemImg.item.eq(cartItem.item),
                        itemImg.representImgYn.eq(TRUE)
                        )
                .orderBy(cartItem.regTime.desc())
                .fetch();
    }
}
