package com.nalepa.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;


@SpringBootApplication(exclude = {MongoAutoConfiguration.class})
public class SEDA_06_WebFLUX_PlatformThreads_MONGO {

    public static void main(String[] args) {
        SpringApplication.run(SEDA_06_WebFLUX_PlatformThreads_MONGO.class, args);
    }
}