package com.shop.domain.order.repository;

import com.shop.application.order.ItemService;
import com.shop.application.order.dto.ItemFormDto;
import com.shop.domain.order.ItemImg;
import com.shop.domain.order.model.Item;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(args = "--spring.profiles.active=test")
@Transactional
class ItemImgRepositoryTest {


    @Autowired
    ItemService itemService;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemImgRepository itemImgRepository;

    @Value("itemImgLocation")
    String path;

    List<MultipartFile> createMultipartFiles() {
        return IntStream.range(0, 5)
                .mapToObj(i -> {
                    String imageName = "image" + i + ".jpg";
                    return new MockMultipartFile(path, imageName, "image/jpg", new byte[]{1, 2, 3, 4});
                })
                .collect(Collectors.toList());
    }

    @Test
    @DisplayName("상품 등록 테스트")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void saveItem () throws Exception {
        // given
        final ItemFormDto itemFormDto = ItemFormDto.builder()
                .itemName("테스트상품")
                .itemSellStatus(ItemSellStatus.SELL)
                .itemDetail("테스트 상품 입니다.")
                .price(1000)
                .stockNumber(100)
                .build();

        final List<MultipartFile> multipartFiles = createMultipartFiles();

        // when
        final Long itemId = itemService.saveItem(itemFormDto, multipartFiles);
        final List<ItemImg> itemImgList = itemImgRepository.findByItemIdOrderByIdAsc(itemId);
        final Item item = itemRepository.findById(itemId)
                .orElseThrow(EntityNotFoundException::new);

        // then
        assertThat(item.getItemName()).isEqualTo(itemFormDto.getItemName());
        assertThat(item.getItemDetail()).isEqualTo(itemFormDto.getItemDetail());
        assertThat(item.getPrice()).isEqualTo(itemFormDto.getPrice());
        assertThat(item.getStockNumber()).isEqualTo(itemFormDto.getStockNumber());
        assertThat(item.getItemSellStatus()).isEqualTo(itemFormDto.getItemSellStatus());
        assertThat(multipartFiles.get(0).getOriginalFilename()).isEqualTo(itemImgList.get(0).getOriginImgName());
    }


}