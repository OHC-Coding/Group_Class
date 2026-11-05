package com.ohc.groupclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GroupClassApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroupClassApplication.class, args);
    }
}
