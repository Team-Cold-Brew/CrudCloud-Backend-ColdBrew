package com.riwi.CrudCloud;

import com.riwi.CrudCloud.database.config.SharedContainerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SharedContainerConfig.class)
public class CrudCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrudCloudApplication.class, args);
	}

}
