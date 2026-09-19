package com.mmu.repository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RepositoryApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(RepositoryApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace(); // This will force the full error to print
        }
    }
}

  