package com.dammak.notification_service.config;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter emailSuccessCounter(MeterRegistry registry) {
        return Counter.builder("notification.email.success")
                .description("Number of emails successfully sent")
                .register(registry);
    }

    @Bean
    public Counter emailFailureCounter(MeterRegistry registry) {
        return Counter.builder("notification.email.failure")
                .description("Number of emails that failed to send")
                .register(registry);
    }

    @Bean
    public Counter smsSuccessCounter(MeterRegistry registry) {
        return Counter.builder("notification.sms.success")
                .description("Number of SMS successfully sent")
                .register(registry);
    }

    @Bean
    public Counter smsFailureCounter(MeterRegistry registry) {
        return Counter.builder("notification.sms.failure")
                .description("Number of SMS that failed to send")
                .register(registry);
    }

    @Bean
    public Counter pushSuccessCounter(MeterRegistry registry) {
        return Counter.builder("notification.push.success")
                .description("Number of push notifications successfully sent")
                .register(registry);
    }

    @Bean
    public Counter pushFailureCounter(MeterRegistry registry) {
        return Counter.builder("notification.push.failure")
                .description("Number of push notifications that failed to send")
                .register(registry);
    }
}