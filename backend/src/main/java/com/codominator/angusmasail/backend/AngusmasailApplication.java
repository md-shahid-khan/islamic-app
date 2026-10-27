package com.codominator.angusmasail.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AngusmasailApplication {

    public static void main(String[] args) {
        SpringApplication.run(AngusmasailApplication.class, args);
    }
}
