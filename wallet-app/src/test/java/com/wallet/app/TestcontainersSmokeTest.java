package com.wallet.app;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testcontainers 冒烟示例：起真实 MySQL 8 / Redis 7 容器验证连通。
 * 集成测试范式参考本类；本机没有 Docker 时整类自动跳过（disabledWithoutDocker）。
 */
@Testcontainers(disabledWithoutDocker = true)
class TestcontainersSmokeTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("wallet");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Test
    void mysqlContainerWorks() throws Exception {
        try (Connection conn = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(),
            MYSQL.getPassword());
             ResultSet rs = conn.createStatement().executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void redisContainerWorks() {
        assertTrue(REDIS.isRunning());
        assertTrue(REDIS.getMappedPort(6379) > 0);
    }
}
