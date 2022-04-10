package com.shop.application.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    public String uploadFile(String uploadPath, String originalFileName, MultipartFile file) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String savedFileName = now + originalFileName + extension;
        String fileUploadFullUrl = uploadPath + File.separator + savedFileName;

        file.transferTo(new File(fileUploadFullUrl));
        return savedFileName;
    }

    public void deleteFiles(String filePath) throws IOException {
        final boolean result = Files.deleteIfExists(Path.of(filePath));
        if (result) {
            String fileName = filePath.substring(filePath.lastIndexOf("/"));
            log.info("{} 파일이 정상적으로 삭제 되었습니다.", fileName);
        }
        throw new FileNotFoundException("파일이 존재하지 않습니다.");
    }
}
