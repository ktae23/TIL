package com.shop.application.order;

import com.shop.application.order.dto.ItemFormDto;
import com.shop.application.order.dto.ItemImgDto;
import com.shop.application.order.dto.ItemSearch;
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
import java.util.stream.IntStream;

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
        final List<Item> itemList = itemRepository.getAdminItemWithSearchCondition(itemSearch, pageable);
        return orderAndPaging(itemList, pageable);

    }

    public static Page<Item> orderAndPaging(List<Item> itemList, Pageable pageable) {
        List<Item> content = new ArrayList<>();
        long total = itemList.size();

        // 빈 객체가 아닐 경우
        if (isNotNullOrEmpty(itemList)) {
            int pageSize = pageable.getPageSize();
            int pageNumber = pageable.getPageNumber() + 1;
            int offset = 0;
            int limit = itemList.size();

            // 아이디 기준 역순 정렬
            final List<Item> sortedDesc = itemList.stream()
                    .sorted((i1, i2) -> (int) (i2.getId() - i1.getId()))
                    .collect(Collectors.toList());

            // 요청한 페이지가 첫 페이지가 아닐 경우 시작 인덱스
            if (pageNumber > 1) {
                offset = (pageSize * pageNumber) - pageSize;
            }

            // 페이지가 2 이상 존재하면서
            if (itemList.size() > pageSize) {
                // 첫 페이지일 경우 종료 인덱스
                if (offset == 0) {
                    limit = pageSize;

                }  // 마지막 페이지인 경우 종료 인덱스
                else if (itemList.size() > offset && limit > itemList.size()) {
                    limit = itemList.size();

                }  // 첫 페이지가 아닌 경우 종료 인덱스
                else {
                    limit = pageSize * pageNumber;
                }
            }

            content =
                    IntStream.range(offset, limit)
                            .mapToObj(sortedDesc::get)
                            .collect(Collectors.toList());
        }
        return new PageImpl<>(content, pageable, total);
    }
}
