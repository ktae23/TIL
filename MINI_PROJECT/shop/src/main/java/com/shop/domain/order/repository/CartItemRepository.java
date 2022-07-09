package com.shop.domain.order.repository;

import com.shop.domain.order.model.CartItem;
import com.shop.domain.order.repository.custom.CartItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemRepositoryCustom {

    Optional<CartItem> findByCartIdAndItemId(Long cartId, Long itemId);



}
