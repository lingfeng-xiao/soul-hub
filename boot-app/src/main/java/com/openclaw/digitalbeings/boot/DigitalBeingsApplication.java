package com.openclaw.digitalbeings.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.openclaw.digitalbeings")
@EnableScheduling
public class DigitalBeingsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalBeingsApplication.class, args);
    }
}
