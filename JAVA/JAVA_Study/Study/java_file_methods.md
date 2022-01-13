```java
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;


@RequiredArgsConstructor
@Component
public class LcFile {

    private final Tika tika;

    public String getNewFileName(String fileName) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        String now = currentDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss"));

        StringBuilder sb = new StringBuilder();

        sb.append(now);
        sb.append("_");
        sb.append(convertSpaceToUnderScore(fileName));
        return sb.toString();
    }

    public List<MultipartFile> getMultipartFileNames(MultipartHttpServletRequest request) {
        Iterator<String> fileNames = request.getFileNames();
        List<MultipartFile> multipartFiles = new ArrayList<>();

        while (fileNames.hasNext()) {
            String fileName = fileNames.next();
            multipartFiles.add(request.getFile(fileName));
        }
        return multipartFiles;
    }


    public Optional<File> getFile(Object fileEntity) throws IOException {
        String fileLocation = "";
        String fileName = "";
        Optional<File> file = Optional.empty();

        if (fileEntity instanceof LcTaskFileEntity) {
            LcTaskFileEntity lcTaskFileEntity = (LcTaskFileEntity) fileEntity;
            fileLocation = "/home/ubuntu/tasks/" + lcTaskFileEntity.getClassId() + File.separator + lcTaskFileEntity.getSubClassId();
            fileName = lcTaskFileEntity.getNewFileName();
        }
        
        if (fileEntity instanceof LcFileEntity) {
            LcFileEntity lcFileEntity = (LcFileEntity) fileEntity;
            fileLocation = "/home/ubuntu/files/" + lcFileEntity.getClassId();
            fileName = lcFileEntity.getNewFileName();
        }

        String filePath = fileLocation + File.separator + fileName;
        makeDirectories(fileLocation);

        file = Optional.of(new File(filePath));

        if (file.isPresent()) {
            String mimeType = tika.detect(file.get());
            if (!mimeTypeChecker(mimeType)) {
                return Optional.empty();
            }
        }

        return Optional.ofNullable(file.get());
    }

    private void makeDirectories(String fileLocation) {

        File directory = new File(fileLocation);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }


    public void saveFiles(Object fileDto, List<MultipartFile> multipartFiles, List<String> newFileNameList) throws IOException {

        String fileLocation = "";

        if (fileDto instanceof LcSubmitTaskDto) {
            LcSubmitTaskDto lcSubmitTaskDto = (LcSubmitTaskDto) fileDto;
            fileLocation = "/home/ubuntu/submit_task/" +  + lcSubmitTaskDto.classId + File.separator + lcSubmitTaskDto.subClassId;
        }

        makeDirectories(fileLocation);

        for (int i = 0; i < multipartFiles.size(); i++) {
            InputStream is = multipartFiles.get(i).getInputStream();
            String mimeType = tika.detect(is);

            if (mimeTypeChecker(mimeType)) {
                String filePath = fileLocation + File.separator + convertSpaceToUnderScore(newFileNameList.get(i));
                multipartFiles.get(i).transferTo(Paths.get(filePath));

            }
        }
    }


    public void deleteFiles(List<Object> fileEntityList) throws IOException {

        String fileLocation = "";
        String fileName = "";
        for (Object entity : fileEntityList) {

            if (entity instanceof FileEntity) {
                FileEntity fileEntity = (FileEntity) entity;
                fileLocation = "/home/ubuntu/files/" + FileEntity.getClassId();
                fileName = fileEntity.getNewFileName();
            }

            if (entity instanceof taskFileEntity) {
                TaskFileEntity taskFileEntity = (TaskFileEntity) entity;
                fileLocation = "/home/ubuntu/tasks/" + taskFileEntity.getClassId() + File.separator + taskFileEntity.getSubClassId();
                fileName = taskFileEntity.getNewFileName();
            }

            String filePath = fileLocation + File.separator + fileName;

            File targetFile = new File(filePath);

            targetFile.delete();


        }

        if (isEmpty(Paths.get(fileLocation))) {
            File directories = new File(fileLocation);
            directories.delete();
        }
    }

    public boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }

        return false;
    }


    public ResponseEntity<Resource> getFileDownload(Optional<File> file, HttpServletRequest request) throws IOException {

        HttpHeaders header = new HttpHeaders();

        Resource resource = new InputStreamResource(new FileInputStream(file.get()));

        String encodedFileName = fileNameEncoder(file.get().getName(), request);

        header.add("Content-Disposition", "attachment; filename=" + "\"" + encodedFileName + "\"");
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        return ResponseEntity.status(HttpStatus.OK)
            .headers(header)
            .contentLength(file.get().length())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }


    public boolean mimeTypeChecker(@NotBlank String mimeType) {

        boolean result;

        String startsWith = "image,text,application/vnd.ms-word,application/vnd.ms-excel,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument,applicaion/vnd.hancom"
            
        String equalType = "jpg,jpeg,png,zip,msword,docx,x-hwp,ppt,hannsofthwp,x-tika-msoffice,x-tika-ooxml,pptx,excel,xlsx,pdf,md,gif,tar"

        List<String> acceptMimeTypeStartsWith = Arrays.asList(startsWith.split(","));
        List<String> acceptMimeTypeList = Arrays.asList(equalType.split(","));

        result = acceptMimeTypeStartsWith.stream().anyMatch(type -> mimeType.startsWith(type));

        if (result) {
            return true;
        }

        return acceptMimeTypeList.stream().anyMatch(type -> mimeType.equals("application/" + type));
    }

    public String fileNameEncoder(String fileName, HttpServletRequest request) throws UnsupportedEncodingException {

        String requestHeader = request.getHeader("User-Agent");
        String encodedFileName = "";
        String originalFileName = getOriginalFileName(URLDecoder.decode(fileName, "UTF-8"));

        if (requestHeader.contains("Edge")) {
            encodedFileName = URLEncoder.encode(originalFileName, "UTF-8").replaceAll("\\+", "%20");
        } else if (requestHeader.contains("MSIE") || requestHeader.contains("Trident")) {
            encodedFileName = URLEncoder.encode(originalFileName, "UTF-8").replaceAll("\\+", "%20");
        } else if (requestHeader.contains("Chrome")) {
            encodedFileName = new String(originalFileName.getBytes("UTF-8"), "ISO-8859-1");
        } else if (requestHeader.contains("Opera")) {
            encodedFileName = new String(originalFileName.getBytes("UTF-8"), "ISO-8859-1");
        } else if (requestHeader.contains("Firefox")) {
            encodedFileName = new String(originalFileName.getBytes("UTF-8"), "ISO-8859-1");
        }
        return encodedFileName;
    }


    public String getOriginalFileName(String fileName) {
        List<Integer> indexes = findIndexes(fileName);
        return fileName.substring(indexes.get(1) + 1);
    }


    public List<Integer> findIndexes(String fileName) {
        List<Integer> indexList = new ArrayList<Integer>();
        int index = fileName.indexOf("_");
        while (index != -1) {
            indexList.add(index);
            index = fileName.indexOf("_", index + 1);
        }
        return indexList;
    }

    public File convertMultiPartToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(multipartFile.getOriginalFilename());
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(multipartFile.getBytes());
        fos.close();
        return file;
    }

    public String convertSpaceToUnderScore(String value) {
        return value.replaceAll(" ", "_").toLowerCase();
    }

}
```