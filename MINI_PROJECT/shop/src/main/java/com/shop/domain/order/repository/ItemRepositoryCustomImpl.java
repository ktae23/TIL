package com.shop.domain.order.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.application.order.dto.ItemSearch;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.repository.custom.ItemRepositoryCustom;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.shop.domain.order.model.QItem.item;


@Component
@RequiredArgsConstructor
public class ItemRepositoryCustomImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Item> getAdminItemPage(ItemSearch itemSeach, Pageable pageable) {
        List<Item> content = queryFactory
                .selectFrom(item)
                .where(regDtsAfter(itemSeach.getSearchDateType()),
                        searchSellStatusEq(itemSeach.getSearchSellStatus()),
                        searchByLike(itemSeach.getSearchBy(), itemSeach.getSearchQuery()))
                .orderBy(item.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = content.size();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression searchSellStatusEq(ItemSellStatus searchSellStatus) {
        return searchSellStatus == null ? null : item.itemSellStatus.eq(searchSellStatus);
    }

    private BooleanExpression regDtsAfter(String searchDateType) {
        LocalDateTime dateTime = LocalDateTime.now();

        if (!StringUtils.hasText(searchDateType) || "all".equals(searchDateType)) {
            return null;
        }
        switch (searchDateType) {
            case "1d":
                dateTime = dateTime.minusDays(1L);
                break;
            case "1w":
                dateTime = dateTime.minusWeeks(1L);
                break;
            case "1m":
                dateTime = dateTime.minusMonths(1L);
                break;
            case "6m":
                dateTime = dateTime.minusMonths(6L);
                break;
            default:
                return null;
        }
        return item.regTime.after(dateTime);
    }

    private BooleanExpression searchByLike(String searchBy, String searchQuery) {
        switch (searchBy) {
            case "itemNm":
                return item.itemName.like("%" + searchQuery + "%");
            case "createdBy":
                return item.createdBy.like("%" + searchQuery + "%");
            default:
                return null;
        }
    }
}
