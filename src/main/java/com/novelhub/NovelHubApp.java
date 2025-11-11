package com.novelhub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Novel Hub Application
 * A backend service for novel management with user authentication and fingerprint validation
 */
@Slf4j
@SpringBootApplication
public class NovelHubApp {
    public static void main(String[] args) {
        SpringApplication.run(NovelHubApp.class, args);
        log.info("====== NovelHub Application Started Successfully ======");
    }
}

