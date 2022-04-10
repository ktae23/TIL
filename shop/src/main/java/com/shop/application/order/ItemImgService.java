package com.shop.application.order;

import com.shop.domain.order.ItemImg;
import com.shop.domain.order.repository.ItemImgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemImgService {

    @Value("${itemImgLocation}")
    private String itemImgLocation;
    private final ItemImgRepository itemImgRepository;
    private final FileService fileService;

    public void saveItemImg(ItemImg itemImg, MultipartFile file) throws IOException {
        String originImgName = file.getOriginalFilename();
        String imgName = "";
        String imgUrl = "";

        if (StringUtils.hasText(originImgName)) {
            imgName = fileService.uploadFile(itemImgLocation, originImgName, file);
            imgUrl = String.format("/images/item/%s", imgName);
        }

        itemImg.updateItemImg(originImgName, imgName, imgUrl);
        itemImgRepository.save(itemImg);
    }


    public void updateItemImg(Long itemImgId, MultipartFile itemImgFile) throws IOException {
        if (!itemImgFile.isEmpty()) {
            final ItemImg savedItemImg = itemImgRepository.findById(itemImgId)
                    .orElseThrow(EntityNotFoundException::new);
            if (StringUtils.hasText(savedItemImg.getImgName())
            && !savedItemImg.getOriginImgName().equals(itemImgFile.getName())) {
                fileService.deleteFiles(itemImgLocation + "/" + savedItemImg.getImgName() );
            }

            String originImgName = itemImgFile.getOriginalFilename();
            String imgName = fileService.uploadFile(itemImgLocation, originImgName, itemImgFile);
            String imgUrl = "/image/item/" + imgName;
            savedItemImg.updateItemImg(originImgName, imgName, imgUrl);
        }
    }
}
