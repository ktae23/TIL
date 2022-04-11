package com.shop.domain.order.repository.custom;

import com.shop.application.order.dto.ItemSearch;
import com.shop.domain.order.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ItemRepositoryCustom {

    Page<Item> getAdminItemPage(ItemSearch itemSearch, Pageable pageable);
}
