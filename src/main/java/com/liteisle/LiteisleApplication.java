package com.liteisle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.liteisle.mapper")
@SpringBootApplication
public class LiteisleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteisleApplication.class, args);
    }

}
