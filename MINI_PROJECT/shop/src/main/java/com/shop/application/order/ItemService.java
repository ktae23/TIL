package com.shop.application.order;

import com.shop.application.order.dto.ItemFormDto;
import com.shop.application.order.dto.ItemImgDto;
import com.shop.application.order.dto.ItemSearch;
import com.shop.application.order.dto.MainItemDto;
import com.shop.domain.order.ItemImg;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.repository.ItemImgRepository;
import com.shop.domain.order.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.shop.infrastructure.utils.Utils.isNotNullOrEmpty;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemImgService itemImgService;
    private final ItemImgRepository itemImgRepository;

    public Long saveItem(ItemFormDto itemFormDto, List<MultipartFile> itemImgFileList) throws IOException {
        final Item item = Item.from(itemFormDto);
        itemRepository.save(item);
        for (int i = 0; i < itemImgFileList.size(); i++) {
            ItemImg itemImg = ItemImg.builder().item(item).representImgYn(FALSE).build();
            if (i == 0) {
                itemImg.changeRepresentImgStatus(TRUE);
            }
            itemImgService.saveItemImg(itemImg, itemImgFileList.get(i));
        }
        return item.getId();
    }

    @Transactional(readOnly = true)
    public ItemFormDto getItemDto(Long itemId) {
        final List<ItemImg> itemImgList = itemImgRepository.findByItemIdOrderByIdAsc(itemId);
        final List<ItemImgDto> itemImgDtoList = itemImgList.stream()
                .map(ItemImgDto::of)
                .collect(Collectors.toList());

        final Item item = itemRepository.findById(itemId).
                orElseThrow(EntityNotFoundException::new);

        final ItemFormDto itemFormDto = ItemFormDto.of(item);
        itemFormDto.setItemImgDtoList(itemImgDtoList);
        return itemFormDto;
    }


    public Long updateItem(ItemFormDto itemFormDto, List<MultipartFile> itemImgFileList) throws IOException {
        final Item item = itemRepository.findById(itemFormDto.getId())
                .orElseThrow(EntityNotFoundException::new);
        item.updateItem(itemFormDto);

        final List<Long> itemImgIds = itemFormDto.getItemImgIds();
        for (int i = 0; i < itemImgFileList.size(); i++) {
            itemImgService.updateItemImg(itemImgIds.get(i), itemImgFileList.get(i));
        }
        return item.getId();
    }



    @Transactional(readOnly = true)
    public Page<Item> getAdminItemPage(ItemSearch itemSearch, Pageable pageable) {
        final List<Item> itemList = itemRepository.getAdminItemWithSearchCondition(itemSearch);
        return pagination(itemList, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MainItemDto> getMainItemPage(ItemSearch itemSearch, Pageable pageable) {
        final List<MainItemDto> mainItemDtoList = itemRepository.getMainItemPage(itemSearch);
        return pagination(mainItemDtoList, pageable);
    }
    public static <T> Page<T> pagination(List<T> entityList, Pageable pageable) {
        List<T> content = new ArrayList<>();
        long total = entityList.size();

        // 빈 객체가 아닐 경우
        if (isNotNullOrEmpty(entityList)) {
            int pageSize = pageable.getPageSize();
            int pageNumber = pageable.getPageNumber() + 1;

            int offset = (pageSize * pageNumber) - pageSize;
            int limit = pageSize * pageNumber;


            // 마지막 페이지인 경우 종료 인덱스
            if (limit >= entityList.size()) {
                limit = entityList.size();
            }
            content = entityList.subList(offset, limit);
        }
        return new PageImpl<>(content, pageable, total);
    }
}
