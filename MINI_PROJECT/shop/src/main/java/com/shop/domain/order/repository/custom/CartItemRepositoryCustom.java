package com.shop.domain.order.repository.custom;

import com.shop.application.order.dto.CartDetailDto;

import java.util.List;

public interface CartItemRepositoryCustom {

    List<CartDetailDto> findCartDetailDtoList(Long cartId);
}
