package com.tutorsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TutorSysApplication {
    public static void main(String[] args) {
        SpringApplication.run(TutorSysApplication.class, args);
    }
}
