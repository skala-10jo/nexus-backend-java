package com.langapp.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LangappApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangappApplication.class, args);
        System.out.println("🚀 Server is running on http://localhost:3000");
        System.out.println("📝 Environment: development");
        System.out.println("🌐 CORS enabled for: http://localhost:5173");
    }
}
