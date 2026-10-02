package com.demo;

import com.demo.traceusage.TraceUsageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TraceUsageProperties.class)
public class TraceUsageDemoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceUsageDemoApiApplication.class, args);
    }
}
