package io.vertx.example;

import io.vertx.core.AbstractVerticle;

/**
 *
 * @author kelc
 * @date 24/02/2021
 */
public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {
        vertx.deployVerticle(Downloader.class.getName());
    }
}
