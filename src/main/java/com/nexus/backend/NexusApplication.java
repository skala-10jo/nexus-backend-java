package com.nexus.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class NexusApplication {

    @PostConstruct
    public void init() {
        // JVM 타임존을 Asia/Seoul로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(NexusApplication.class, args);
        System.out.println("🚀 Server is running on http://localhost:3000");
        System.out.println("📝 Environment: development");
        System.out.println("🌐 CORS enabled for: http://localhost:5173");
        System.out.println("🕐 Timezone: " + TimeZone.getDefault().getID());
    }
}
