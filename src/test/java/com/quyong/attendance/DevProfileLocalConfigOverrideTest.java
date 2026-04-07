package com.quyong.attendance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevProfileLocalConfigOverrideTest {

    @Test
    void shouldPreferRootApplicationLocalOverridesOverDevDefaults() throws IOException {
        String originalUserDir = System.getProperty("user.dir");
        Path worktreeRoot = Paths.get(originalUserDir);
        Path temporaryRoot = Files.createTempDirectory(worktreeRoot, "local-config-");
        Path applicationLocalFile = temporaryRoot.resolve("application-local.yml");
        ConfigurableEnvironment environment = new StandardEnvironment();

        Files.write(
                applicationLocalFile,
                Arrays.asList(
                        "spring:",
                        "  redis:",
                        "    host: root-local-redis-host",
                        "    password: ~",
                        "  datasource:",
                        "    username: root-local-db-user",
                        ""
                ),
                StandardCharsets.UTF_8
        );

        // 通过隔离 user.dir，避免覆盖开发者私有本地配置，同时复现相同的本地导入路径。
        System.setProperty("user.dir", temporaryRoot.toString());
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AttendanceApplication.class)
                .environment(environment)
                .profiles("dev")
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.main.lazy-initialization=true",
                        "logging.level.root=OFF"
                )
                .run()) {
            String redisPassword = context.getEnvironment().getProperty("spring.redis.password");

            assertAll(
                    () -> assertEquals("root-local-redis-host", context.getEnvironment().getProperty("spring.redis.host")),
                    () -> assertEquals("root-local-db-user", context.getEnvironment().getProperty("spring.datasource.username")),
                    () -> assertNotEquals("change_me", redisPassword),
                    () -> assertTrue(redisPassword == null || redisPassword.isEmpty())
            );
        } finally {
            System.setProperty("user.dir", originalUserDir);
            deleteRecursively(temporaryRoot);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        }
    }
}
