package com.jooyeon.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClientManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientManagementApplication.class, args);
    }

}