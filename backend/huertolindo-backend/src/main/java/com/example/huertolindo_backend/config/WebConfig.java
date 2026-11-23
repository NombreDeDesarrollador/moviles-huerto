package com.example.huertolindo_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Path uploadDirectory;
    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.upload-dir:uploads}") String uploadDirectory,
                     @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDirectory.toUri().toString() + (uploadDirectory.toUri().toString().endsWith("/") ? "" : "/"));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
