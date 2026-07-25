package com.itpassport.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ItPassportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItPassportApplication.class, args);
    }
}
