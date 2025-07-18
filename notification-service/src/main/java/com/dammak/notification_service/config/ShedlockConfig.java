//package com.dammak.notification_service.config;
//
//
//import net.javacrumbs.shedlock.core.LockProvider;
//import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
//import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import javax.sql.DataSource;
//
//@Configuration
//@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
//public class ShedlockConfig {
//
//    @Bean
//    public LockProvider lockProvider(DataSource dataSource) {
////        return new JdbcTemplateLockProvider(dataSource);
//
//        return new JdbcTemplateLockProvider(
//                JdbcTemplateLockProvider.Configuration.builder()
//                        .withJdbcTemplate(new JdbcTemplate(dataSource))
//                        .usingDbTime()
//                        .build()
//        );
//    }
//}