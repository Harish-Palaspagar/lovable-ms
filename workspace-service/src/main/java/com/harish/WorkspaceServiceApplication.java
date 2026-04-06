package com.harish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class WorkspaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceServiceApplication.class, args);
    }

}
