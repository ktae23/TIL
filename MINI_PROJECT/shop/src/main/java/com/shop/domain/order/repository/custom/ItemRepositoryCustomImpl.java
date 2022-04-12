package com.shop.domain.order.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.application.order.dto.ItemSearch;
import com.shop.application.order.dto.MainItemDto;
import com.shop.application.order.dto.QMainItemDto;
import com.shop.domain.order.model.Item;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.shop.domain.order.QItemImg.itemImg;
import static com.shop.domain.order.model.QItem.item;


@RequiredArgsConstructor
public class ItemRepositoryCustomImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Item> getAdminItemWithSearchCondition(ItemSearch itemSearch) {
        return queryFactory
                .selectFrom(item)
                .where(regDtsAfter(itemSearch.getSearchDateType()),
                        searchSellStatusEq(itemSearch.getSearchSellStatus()),
                        searchByLike(itemSearch.getSearchBy(), itemSearch.getSearchQuery()))
                .orderBy(item.id.desc())
                .fetch();
    }

    @Override
    public List<MainItemDto> getMainItemPage(ItemSearch itemSearch) {
        return queryFactory
                .select(new QMainItemDto(
                        item.id,
                        item.itemName,
                        item.itemDetail,
                        itemImg.imgUrl,
                        item.price
                ))
                .from(itemImg)
                .join(itemImg.item, item)
                .where(itemImg.representImgYn.eq(true))
                .where(itemNameLike(itemSearch.getSearchQuery()))
                .orderBy(item.id.desc())
                .fetch();

    }

    private BooleanExpression itemNameLike(String searchQuery){
        return StringUtils.hasText(searchQuery) ? item.itemName.eq("%" + searchQuery + "%") : null;
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
        if (!StringUtils.hasText(searchBy)) {
            return null;
        }
        switch (searchBy) {
            case "itemName":
                return item.itemName.like("%" + searchQuery + "%");
            case "createdBy":
                return item.createdBy.like("%" + searchQuery + "%");
            default:
                return null;
        }
    }
}
