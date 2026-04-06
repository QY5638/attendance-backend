package com.quyong.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DatabaseIntegrationBaselineTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadDataSourceAndSeedDepartmentDataForIntegrationTests() throws Exception {
        DataSource dataSource = applicationContext.getBeanProvider(DataSource.class).getIfAvailable();

        assertNotNull(dataSource, "测试环境应提供 DataSource");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Integer departmentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM department", Integer.class);

        assertEquals(Integer.valueOf(3), departmentCount);
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}
