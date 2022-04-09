package com.shop.repository;

import com.querydsl.core.BooleanBuilder;
import com.shop.constant.ItemSellStatus;
import com.shop.entity.Item;
import com.shop.entity.QItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(args = "--spring.profiles.active=test")
class ItemQueryRepositoryTest {

    @Autowired
    ItemRepository itemRepository;


    @PostConstruct
    void createItemList() {
        for (int i = 0; i < 5; i++) {
            Item item = Item.builder()
                    .itemName("테스트 상품" + i)
                    .price(10000 + i)
                    .stockNumber(100)
                    .itemDetail("테스트 상품 상세 설명" + i)
                    .itemSellStatus(ItemSellStatus.SELL)
                    .regTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            itemRepository.save(item);
        }
        for (int i = 5; i < 10; i++) {
            Item item = Item.builder()
                    .itemName("테스트 상품" + i)
                    .price(10000 + i)
                    .stockNumber(0)
                    .itemDetail("테스트 상품 상세 설명" + i)
                    .itemSellStatus(ItemSellStatus.SOLD_OUT)
                    .regTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            itemRepository.save(item);
        }
    }

    @Test
    @DisplayName("상품 Querydsl 조회 테스트 2")
    void queryDslTest2() {
        // given
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        QItem item = QItem.item;
        String itemDetail = "테스트 상품 상세 설명";
        int price = 1003;
        String itemSellStatus = "SELL";

        // when
        booleanBuilder.and(item.itemDetail.like("%" + itemDetail + "%"));
        booleanBuilder.and(item.price.gt(price));
        if (StringUtils.equals(itemSellStatus, ItemSellStatus.SELL)) {
            booleanBuilder.and(item.itemSellStatus.eq(ItemSellStatus.SELL));
        }

        PageRequest pageable = PageRequest.of(0, 5);
        Page<Item> itemPagingResult = itemRepository.findAll(booleanBuilder, pageable);
        List<Item> itemList = itemPagingResult.get().collect(Collectors.toList());
        // then
        Optional<Item> fistItem = itemRepository.findById(1L);
        Optional<Item> fifthItem = itemRepository.findById(5L);
        if (fistItem.isPresent() && fifthItem.isPresent()) {
            assertThat(itemList.size()).isEqualTo(5);
            assertThat(itemList).contains(fistItem.get(), fifthItem.get());
            return;
        }
        fail("조회 대상이 없습니다.");
    }
}