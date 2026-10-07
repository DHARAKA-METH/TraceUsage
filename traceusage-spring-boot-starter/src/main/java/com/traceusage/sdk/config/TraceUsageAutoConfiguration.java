package com.traceusage.sdk.config;

import com.traceusage.sdk.interceptor.TraceUsageInterceptor;
import com.traceusage.sdk.service.TelemetryPublisher;
import com.traceusage.sdk.telemetry.TelemetryHttpClient;
import com.traceusage.sdk.telemetry.TelemetryQueue;
import com.traceusage.sdk.telemetry.TelemetryWorker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(TraceUsageProperties.class)
@ConditionalOnClass({WebMvcConfigurer.class, HandlerInterceptor.class})
@ConditionalOnProperty(prefix = "traceusage", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "traceusage", name = "api-key")
@ConditionalOnProperty(prefix = "traceusage", name = "server-url")
public class TraceUsageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TelemetryQueue telemetryQueue(TraceUsageProperties properties) {
        return new TelemetryQueue(properties.getQueueCapacity());
    }

    @Bean
    @ConditionalOnMissingBean
    public TelemetryHttpClient telemetryHttpClient(TraceUsageProperties properties) {
        return new TelemetryHttpClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TelemetryPublisher telemetryPublisher(TelemetryQueue queue, TraceUsageProperties properties) {
        return new TelemetryPublisher(queue, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TelemetryWorker telemetryWorker(TelemetryQueue queue,
                                           TelemetryHttpClient httpClient,
                                           TraceUsageProperties properties) {
        return new TelemetryWorker(queue, httpClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceUsageInterceptor traceUsageInterceptor(TelemetryPublisher publisher) {
        return new TraceUsageInterceptor(publisher);
    }

    @Bean
    public WebMvcConfigurer traceUsageWebMvcConfigurer(TraceUsageInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .addPathPatterns("/api/**");
            }
        };
    }
}
