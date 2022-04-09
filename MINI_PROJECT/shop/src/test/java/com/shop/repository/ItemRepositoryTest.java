package com.shop.repository;

import com.shop.constant.ItemSellStatus;
import com.shop.entity.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(args = "--spring.profiles.active=test")
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    @Test
    @DisplayName("상품 저장 테스트")
    void createItemTest() {
        Item item = Item.builder()
                .itemName("테스트 상품")
                .price(10000)
                .stockNumber(100)
                .itemDetail("테스트 상품 상세 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .regTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        Item savedItem = itemRepository.save(item);
        assertThat(item).isEqualTo(savedItem);
    }

    @PostConstruct
    void createItemList() {
        for (int i = 0; i < 10; i++) {
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
    }

    @Test
    @DisplayName("상품명 조회 테스트")
    void findByItemNameTest() {
        List<Item> itemList = itemRepository.findByItemName("테스트 상품0");
        Optional<Item> findItem = itemRepository.findById(1L);
        if (findItem.isPresent()) {
            assertThat(itemList).contains(findItem.get());
            return;
        }
        fail("조회 대상이 없습니다.");
    }

    @Test
    @DisplayName("상품명, 상품상세설명 or 테스트")
    void findByItemNameOrItemDetailTest() {
        List<Item> itemList = itemRepository.findByItemNameOrItemDetail("테스트 상품0", "테스트 상품 상세 설명5");
        Optional<Item> fistItem = itemRepository.findById(1L);
        Optional<Item> sixthItem = itemRepository.findById(6L);
        if (fistItem.isPresent() && sixthItem.isPresent()) {
            assertThat(itemList).containsExactly(fistItem.get(), sixthItem.get());
            return;
        }
        fail("조회 대상이 없습니다.");

    }

    @Test
    @DisplayName("가격 LessThan 테스트")
    void findByPriceLessThanTest() {
        List<Item> itemList = itemRepository.findByPriceLessThan(10005);
        Optional<Item> fistItem = itemRepository.findById(1L);
        Optional<Item> secondItem = itemRepository.findById(2L);
        Optional<Item> thirdItem = itemRepository.findById(3L);
        Optional<Item> fourthItem = itemRepository.findById(4L);
        Optional<Item> fifthItem = itemRepository.findById(5L);

        if (fistItem.isPresent()
                && secondItem.isPresent()
                && thirdItem.isPresent()
                && fourthItem.isPresent()
                && fifthItem.isPresent()) {
            assertThat(itemList).containsExactly(fistItem.get(), secondItem.get(), thirdItem.get(), fourthItem.get(), fifthItem.get());
            return;
        }
        fail("조회 대상이 없습니다.");
    }

    @Test
    @DisplayName("가격 내림차순 조회 테스트")
    void findByPriceLessThanOrderByPriceDescTest() {
        List<Item> itemList = itemRepository.findByPriceLessThanOrderByPriceDesc(10005);
        Optional<Item> fistItem = itemRepository.findById(1L);
        Optional<Item> secondItem = itemRepository.findById(2L);
        Optional<Item> thirdItem = itemRepository.findById(3L);
        Optional<Item> fourthItem = itemRepository.findById(4L);
        Optional<Item> fifthItem = itemRepository.findById(5L);

        if (fistItem.isPresent()
                && secondItem.isPresent()
                && thirdItem.isPresent()
                && fourthItem.isPresent()
                && fifthItem.isPresent()) {
            assertThat(itemList.get(0)).isEqualTo(fifthItem.get());
            assertThat(itemList.get(1)).isEqualTo(fourthItem.get());
            assertThat(itemList.get(2)).isEqualTo(thirdItem.get());
            assertThat(itemList.get(3)).isEqualTo(secondItem.get());
            assertThat(itemList.get(4)).isEqualTo(fistItem.get());
            return;
        }
        fail("조회 대상이 없습니다.");
    }

    @Test
    @DisplayName("@Query를 이용한 상품 조회 테스트")
    void findByItemDetailTest() {
        List<Item> itemList = itemRepository.findByItemDetail("테스트 상품 상세 설명");
        Optional<Item> fistItem = itemRepository.findById(1L);
        Optional<Item> fifthItem = itemRepository.findById(5L);
        Optional<Item> tenthItem = itemRepository.findById(10L);
        if (fistItem.isPresent()
                && fifthItem.isPresent()
                && tenthItem.isPresent()
        ) {
            assertThat(itemList.get(0)).isEqualTo(tenthItem.get());
            assertThat(itemList.get(5)).isEqualTo(fifthItem.get());
            assertThat(itemList.get(9)).isEqualTo(fistItem.get());
            assertThat(itemList.size()).isEqualTo(10);
            return;
        }
        fail("조회 대상이 없습니다.");

    }
}