package com.shop.domain.order.repository;

import com.shop.domain.order.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

}
