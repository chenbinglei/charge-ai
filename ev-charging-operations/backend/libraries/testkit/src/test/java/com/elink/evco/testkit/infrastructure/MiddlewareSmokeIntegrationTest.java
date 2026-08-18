package com.elink.evco.testkit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 使用真实容器校验 W1 中间件最小协议边界；没有容器运行时时由 Testcontainers 明确跳过。
 *
 * <p>本测试不会创建业务 Topic、业务消费者、业务 Schema 或业务数据。
 */
@Testcontainers(disabledWithoutDocker = true)
class MiddlewareSmokeIntegrationTest {
    /** W1 固定的 MySQL 交易事实库运行时。 */
    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(
                            DockerImageName.parse(
                                            "mysql:8.4.5@sha256:679e7e924f38a3cbb62a3d7df32924b83f7321a602d3f9f967c01b3df18495d6")
                                    .asCompatibleSubstituteFor("mysql"))
                    .withDatabaseName("evco_foundation")
                    .withUsername("test")
                    .withPassword("test-password");

    /**
     * W1 固定的单节点 KRaft Kafka；与 Compose 配置一致，避免 Testcontainers KafkaContainer 对 advertised.listeners
     * 的非路由地址校验冲突。
     */
    @Container
    static final GenericContainer<?> KAFKA =
            new GenericContainer<>(
                            DockerImageName.parse(
                                    "apache/kafka:3.9.0@sha256:fbc7d7c428e3755cf36518d4976596002477e4c052d1f80b5b9eafd06d0fff2f"))
                    .withExposedPorts(9092)
                    .withEnv("KAFKA_NODE_ID", "1")
                    .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
                    .withEnv(
                            "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                            "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                    .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                    .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@localhost:9093")
                    .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,CONTROLLER://:9093")
                    .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092")
                    .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT")
                    .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
                    .waitingFor(
                            org.testcontainers.containers.wait.strategy.Wait.forLogMessage(
                                            ".*Transition from STARTING to STARTED.*", 1)
                                    .withStartupTimeout(java.time.Duration.ofMinutes(3)));

    /** W1 Redis 只验证缓存协议可用，不写入业务事实。 */
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                            DockerImageName.parse(
                                    "redis:7.4.2@sha256:fbdbaea47b9ae4ecc2082ecdb4e1cea81e32176ffb1dcf643d422ad07427e5d9"))
                    .withExposedPorts(6379);

    /** W1 EMQX 只验证 Broker 状态，不模拟或接入设备业务报文。 */
    @Container
    static final GenericContainer<?> EMQX =
            new GenericContainer<>(
                            DockerImageName.parse(
                                    "emqx/emqx:5.8.2@sha256:76bc406d14107aa73539178f536c05e4df2f1f54388bbbbc9d846599a51317f9"))
                    .withExposedPorts(1883);

    /** 验证四类基础中间件的最小真实协议边界可达。 */
    @Test
    @DisplayName("W1：MySQL、Kafka、Redis 与 EMQX 的最小协议边界可达")
    void verifiesMiddlewareProtocolBoundaries() throws Exception {
        try (var connection =
                        DriverManager.getConnection(
                                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                var statement = connection.createStatement();
                var resultSet = statement.executeQuery("SELECT 1");
                var adminClient =
                        AdminClient.create(
                                Map.of(
                                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                                        KAFKA.getHost() + ":" + KAFKA.getMappedPort(9092)))) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
            assertThat(adminClient.describeCluster().clusterId().get()).isNotBlank();
        }

        assertThat(REDIS.execInContainer("redis-cli", "ping").getStdout()).contains("PONG");
        assertThat(EMQX.execInContainer("emqx", "ctl", "status").getExitCode()).isZero();
    }
}
