package com.university.skillshare_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadConfig.class);
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            String location = uploadPath.toUri().toURL().toString();
            
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(location);
            
            logger.info("File upload directory configured: {}", location);
        } catch (Exception e) {
            logger.error("Failed to configure upload directory", e);
        }
    }
}
