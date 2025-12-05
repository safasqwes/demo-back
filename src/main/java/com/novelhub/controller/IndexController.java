package com.novelhub.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user registration, login, password change, and profile management
 */
@Slf4j
@RestController
public class IndexController {

    @GetMapping("/")
    public ResponseEntity<?> index() {
        log.info("Received index request: authRequest");
        return ResponseEntity.ok().body("ok");
    }

}

