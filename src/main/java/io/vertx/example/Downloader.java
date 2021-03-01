package io.vertx.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.example.utils.FileCompress;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author klc
 */
@Slf4j
public class Downloader extends AbstractVerticle {

    private WebClient client;

    private static final int POOL_SIZE = 100;

    private static final String BASE_URL = "https://diaoshi.dehua-kuyun.com/20200929/17720_0abeb9c1/1000k/hls/index.m3u8";

    private static final String OUTPUT_FILE_NAME = "/Users/klc/Downloads/m3u8/test.mp4";

    private static final int RETRY_TIMES = 5;

    public void getWebClient() {
        WebClientOptions options = new WebClientOptions()
                .setMaxPoolSize(POOL_SIZE)
                .setTrustAll(true)
                .setKeepAlive(true)
                .setIdleTimeout(3000)
                .setProxyOptions(new ProxyOptions()
                        .setType(ProxyType.SOCKS5)
                        .setHost("127.0.0.1")
                        .setPort(7890));
        client = WebClient.create(vertx, options);
    }


    public Future<String> fetchIndexFile(String url) {
        Promise<String> promise = Promise.promise();
        this.client.getAbs(url)
                .as(BodyCodec.string())
                .send()
                .onSuccess(res -> promise.complete(res.body()))
                .onFailure(err -> {
                    log.error("Something went wrong " + err.getCause().getMessage());
                    promise.fail(err.getCause());
                });
        return promise.future();
    }

    /**
     * @param fc
     * @return Future
     */
    public Future<String> fetchFile(FileContent fc, int retryTimes) {
        Promise<String> promise = Promise.promise();
        this.client.getAbs(fc.getUrl())
                .as(BodyCodec.buffer())
                .send()
                .onSuccess(res -> vertx.fileSystem().writeFile(fc.getFilePath(), res.bodyAsBuffer(),  ar -> {
                        if (ar.succeeded()) {
                            promise.complete(fc.getFilePath());
                        } else {
                            log.error(ar.cause().getMessage());
                            promise.fail(ar.cause());
                        }}))
                .onFailure(err -> {
                    if (retryTimes - 1 > 0) {
                        reFetchFile(fc, retryTimes-1, promise);
                    } else {
                        log.error("Download file" + fc.getFilePath() +" error " + err.getMessage());
                        promise.fail(err.getCause());
                    }
                });
        return promise.future();
    }

    /**
     * @param fc
     * @return Future
     */
    public void reFetchFile(FileContent fc, int retryTimes, Promise<String> promise) {
        this.client.getAbs(fc.getUrl())
                .as(BodyCodec.buffer())
                .send()
                .onSuccess(res -> vertx.fileSystem().writeFile(fc.getFilePath(), res.bodyAsBuffer(), ar -> {
                    if (ar.succeeded()) {
                        promise.complete(fc.getFilePath());
                    } else {
                        log.error(ar.cause().getMessage());
                        promise.fail(ar.cause());
                    }
                }))
                .onFailure(err -> {
                    if (retryTimes - 1 > 0) {
                        reFetchFile(fc, retryTimes - 1, promise);
                    } else {
                        log.error("Download file" + fc.getFilePath() + " error " + err.getMessage());
                        promise.fail(err.getCause());
                    }
                });
    }

    @Override
    public void start() {
        getWebClient();
        RequestContent rc = new RequestContent(BASE_URL, OUTPUT_FILE_NAME);
        this.fetchIndexFile(BASE_URL).compose(r -> {
            Promise<List> promise = Promise.promise();
            List<String> itemList = Stream.of(r.split("\n"))
                    .filter(str -> str.endsWith(".ts"))
                    .collect(Collectors.toList());
            List<Future> futureList = new ArrayList<>(itemList.size());

            try {
                rc.setFileContents(itemList);
                for(FileContent fc: rc.getFileContents()) {
                    futureList.add(fetchFile(fc, RETRY_TIMES));
                }
            } catch (MalformedURLException e) {
                log.error(e.getCause().getMessage());
                promise.fail(e.getMessage());
            }

            CompositeFuture.all(futureList).onComplete(ar -> {
                if (ar.succeeded()) {
                    List<String> fileList = new ArrayList<>();
                    for (FileContent fileContent : rc.getFileContents()) {
                        fileList.add(fileContent.getFilePath());
                    }
                    FileCompress.merge(vertx.fileSystem(), rc.getOutputFile(), fileList);
                    promise.complete();
                } else {
                    log.error(ar.cause().getMessage());
                }
            });
            return promise.future();
        }).onSuccess(ar -> {
            rc.removeFiles();
            log.info("finish");
            vertx.close();
        }).onFailure(err -> {
            System.out.println(err.getCause().getMessage());
            vertx.close();
        });

    }
}
