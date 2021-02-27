package io.vertx.example;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author klc
 * @date 26/02/2021
 */
@Data
@Slf4j
public class FileContent {

    private String url;
    private int index;
    private String filePath;

    public FileContent(String url, int index, String filePath) {
        this.url = url;
        this.index = index;
        this.filePath = filePath;
    }

}
