package com.shop.application.order;

import com.shop.application.order.dto.ItemFormDto;
import com.shop.application.order.dto.ItemImgDto;
import com.shop.domain.order.ItemImg;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.repository.ItemImgRepository;
import com.shop.domain.order.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

}
