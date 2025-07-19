package com.liteisle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LiteisleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteisleApplication.class, args);
    }

}
