package com.quyong.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
@Profile("test")
public class TestDataSourceConfig {

    private static final String DATABASE_OPTIONS = ";MODE=MySQL;NON_KEYWORDS=USER,ROLE";

    @Bean(destroyMethod = "shutdown")
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setName("attendance-test-" + UUID.randomUUID() + DATABASE_OPTIONS)
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}
