package com.shop.application.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    public String uploadFile(String uploadPath, String originalFileName, MultipartFile file) throws IOException {
        String formatDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String savedFileName = formatDate + "_" + originalFileName;
        String fileUploadFullUrl = uploadPath + File.separator + savedFileName;
        final File uploadFilePath = new File(uploadPath);

        if (!uploadFilePath.exists()) {
            uploadFilePath.mkdirs();
        }
        file.transferTo(new File(fileUploadFullUrl));

        return savedFileName;
    }

    public void deleteFiles(String filePath) throws IOException {
        boolean result = Files.deleteIfExists(Path.of(filePath));
        if (result) {
            String fileName = filePath.substring(filePath.lastIndexOf("/"));
            log.info("{} 파일이 정상적으로 삭제 되었습니다.", fileName);
        }
        throw new FileNotFoundException("파일을 찾지 못했습니다.");

    }
}
