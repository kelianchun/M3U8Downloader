package io.vertx.example;

import io.vertx.example.utils.FileCompress;
import io.vertx.core.file.FileSystem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author klc
 * @date 26/02/2021
 */
@Data
@Slf4j
public class RequestContent {
    private String outputFile;

    private List<FileContent> fileContents = new ArrayList<>();
    private String baseUrl;

    public RequestContent(String baseUrl, String outputFile) {
        this.baseUrl = baseUrl;
        this.outputFile = outputFile;
    }

    public void setFileContents(List<String> fileList) throws MalformedURLException {
        for (int i = 0; i < fileList.size(); i++) {
            try {
                URL url = new URL(this.baseUrl);
                String fileUrl = new URL(url , fileList.get(i)).toString();
                fileContents.add(new FileContent(fileUrl, i, this.outputFile+ "." + i));
            } catch (MalformedURLException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void removeFiles() {
        for (FileContent fileContent : fileContents) {
            Path filePath = new File(fileContent.getFilePath()).toPath();
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                log.error("Delete file "+ filePath.toString() +"error: "+ e.getMessage());
            }
        }
    }
}
