package io.vertx.example.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;
import lombok.extern.slf4j.Slf4j;


import java.util.List;

/**
 * @author klc
 */
@Slf4j
public class FileCompress {

    private FileCompress() {
        throw new IllegalStateException("Utility class");
    }

    private static Future<AsyncFile> openFile(FileSystem fileSystem, String path, OpenOptions openOptions) {
        Promise<AsyncFile> promise = Promise.promise();
        fileSystem.open(path, openOptions, promise);
        return promise.future();
    }

    private static Future<AsyncFile> append(AsyncFile source, AsyncFile destination) {
        Promise<AsyncFile> promise = Promise.promise();
        Pump pump = Pump.pump(source, destination);
        source.exceptionHandler(promise::fail);
        destination.exceptionHandler(promise::fail);
        source.endHandler(v -> promise.complete(destination));
        pump.start();
        return promise.future();
    }

    public static void merge(FileSystem fileSystem, String destFile, List<String> sources) {
        openFile(fileSystem, destFile, new OpenOptions()).compose(outFile -> {
            Future<AsyncFile> mergeFuture = null;
            for (String source : sources) {
                if (mergeFuture == null) {
                    mergeFuture = openFile(fileSystem, source, new OpenOptions())
                            .compose(sourceFile -> append(sourceFile, outFile));
                } else {
                    mergeFuture = mergeFuture
                            .compose(v -> openFile(fileSystem, source, new OpenOptions())
                                    .compose(sourceFile -> append(sourceFile, outFile)));
                }
            }
            return mergeFuture;
        }).onSuccess(ar -> log.info("Merge File Done!"))
                .onFailure(err -> System.out.println("Merge File Error: "+ err.getCause().getMessage()));
    }

}
