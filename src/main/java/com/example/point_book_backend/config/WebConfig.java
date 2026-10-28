package com.example.point_book_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프론트엔드 앱이 /uploads/사진이름.jpg 로 요청을 보내면
        registry.addResourceHandler("/uploads/**")
                // EC2 서버의 /home/ubuntu/uploads/ 폴더에 저장된 진짜 사진을 꺼내서 보여줍니다.
                .addResourceLocations("file:///home/ubuntu/uploads/");
    }
}