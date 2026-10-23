package com.example.point_book_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct; // 추가
import java.util.TimeZone; // 추가

@SpringBootApplication
public class PointBookBackendApplication {

    // 👇 추가할 부분: 서버가 켜질 때 무조건 한국 시간으로 강제 세팅합니다.
    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(PointBookBackendApplication.class, args);
    }
}