package com.riwi.CrudCloud.instance.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
//import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {

    @Value("${docker.host.url:unix:///var/run/docker.sock}")
    private String dockerHostUrl;

    @Bean
    public DockerClient dockerClient() {

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHostUrl)
                .build();

//        return DockerClientBuilder.getInstance(config)
//                .build();
        return null;
    }
}