package com.backend.backend;

import com.backend.backend.config.ModelProperties;
import com.backend.backend.config.ProcessingProperties;
import com.backend.backend.config.VideoStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({VideoStorageProperties.class, ProcessingProperties.class, ModelProperties.class})
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
