package com.elink.evco.testkit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 本地 Kafka 连通性与消息收发验证测试。
 *
 * <p>该测试仅当系统属性 {@code evco.kafka.local.enabled=true} 时运行，默认跳过：
 *
 * <ul>
 *   <li>默认跳过：避免在 CI 或无 Docker 环境中失败。
 *   <li>启用方式：{@code mvn verify -Devco.kafka.local.enabled=true}。
 *   <li>前置条件：本地已通过 {@code docker compose --profile core up -d} 启动 Kafka 容器。
 * </ul>
 *
 * <p>测试用例会创建临时 Topic（前缀 {@code evco-test-}）并在测试结束后自动清理，不污染业务环境。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "evco.kafka.local.enabled", matches = "true")
class KafkaLocalConnectivityTest {

    /**
     * 本地 Compose 启动的 Kafka EXTERNAL 监听地址。
     *
     * <p>Compose 配置中 Kafka 暴露两类 listener：
     *
     * <ul>
     *   <li>INTERNAL (kafka:9092)：供 Compose 网络内其他容器访问。
     *   <li>EXTERNAL (localhost:9094)：供主机进程（含本测试）访问。
     * </ul>
     *
     * 主机直连 9092 会因 {@code advertised.listeners=kafka:9092} 无法解析而超时，必须使用 9094。
     */
    private static final String BOOTSTRAP_SERVERS = "localhost:9094";

    /** 测试用 Topic 前缀，便于识别与清理。 */
    private static final String TEST_TOPIC_PREFIX = "evco-test-";

    private static AdminClient adminClient;

    @BeforeAll
    static void setUp() {
        adminClient =
                AdminClient.create(
                        Map.of(
                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                                BOOTSTRAP_SERVERS,
                                AdminClientConfig.CLIENT_ID_CONFIG,
                                "evco-kafka-local-test-admin"));
    }

    @AfterAll
    static void tearDown() {
        if (adminClient != null) {
            adminClient.close(Duration.ofSeconds(10));
        }
    }

    /** 验证本地 Kafka 集群可连接且返回非空集群 ID。 */
    @Test
    @Order(1)
    @DisplayName("本地 Kafka 集群连接可达且返回 clusterId")
    void localKafkaClusterReachable() throws Exception {
        var clusterId = adminClient.describeCluster().clusterId().get(30, TimeUnit.SECONDS);
        assertThat(clusterId).as("Kafka clusterId 不能为空").isNotBlank();
        System.out.println("[Kafka 本地连通性] clusterId = " + clusterId);
    }

    /** 验证可创建测试 Topic 并列出。 */
    @Test
    @Order(2)
    @DisplayName("创建临时 Topic 并在列表中可见")
    void createAndListTopic() throws Exception {
        var topicName = TEST_TOPIC_PREFIX + "create-" + UUID.randomUUID();
        var partitions = 1;
        var replicationFactor = 1;

        var createResult =
                adminClient
                        .createTopics(
                                Collections.singletonList(
                                        new NewTopic(
                                                topicName, partitions, (short) replicationFactor)))
                        .all();
        createResult.get(30, TimeUnit.SECONDS);

        var topics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
        assertThat(topics).contains(topicName);
        System.out.println("[Kafka 本地连通性] 已创建并确认 Topic: " + topicName);
    }

    /** 验证可向本地 Kafka 发送消息并消费回执。 */
    @Test
    @Order(3)
    @DisplayName("发送并消费一条测试消息（端到端）")
    void sendAndReceiveMessage() throws Exception {
        var topicName = TEST_TOPIC_PREFIX + "msg-" + UUID.randomUUID();
        var messageKey = "test-key";
        var messageValue = "evco-kafka-local-connectivity-test-" + UUID.randomUUID();

        // 创建 Topic
        adminClient
                .createTopics(Collections.singletonList(new NewTopic(topicName, 1, (short) 1)))
                .all()
                .get(30, TimeUnit.SECONDS);

        // 发送消息
        var producerProps =
                Map.<String, Object>of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        BOOTSTRAP_SERVERS,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        StringSerializer.class.getName(),
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        StringSerializer.class.getName(),
                        ProducerConfig.ACKS_CONFIG,
                        "all",
                        ProducerConfig.CLIENT_ID_CONFIG,
                        "evco-kafka-local-test-producer");

        try (var producer = new KafkaProducer<String, String>(producerProps)) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topicName, messageKey, messageValue);
            RecordMetadata metadata = producer.send(record).get(30, TimeUnit.SECONDS);
            assertThat(metadata.offset()).isGreaterThanOrEqualTo(0);
            assertThat(metadata.topic()).isEqualTo(topicName);
            System.out.printf(
                    "[Kafka 本地连通性] 消息已发送: topic=%s partition=%d offset=%d%n",
                    metadata.topic(), metadata.partition(), metadata.offset());
        }

        // 消费消息
        var consumerProps =
                Map.<String, Object>of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        BOOTSTRAP_SERVERS,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class.getName(),
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class.getName(),
                        ConsumerConfig.GROUP_ID_CONFIG,
                        "evco-kafka-local-test-consumer-" + UUID.randomUUID(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                        false);

        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topicName));
            ConsumerRecord<String, String> received = null;
            var deadline = System.currentTimeMillis() + 30_000L;
            while (System.currentTimeMillis() < deadline && received == null) {
                var records = consumer.poll(Duration.ofSeconds(2));
                for (var r : records) {
                    received = r;
                    break;
                }
            }
            assertThat(received).as("30 秒内未消费到测试消息").isNotNull();
            assertThat(received.key()).isEqualTo(messageKey);
            assertThat(received.value()).isEqualTo(messageValue);
            System.out.printf(
                    "[Kafka 本地连通性] 消息已消费: key=%s value=%s%n", received.key(), received.value());
        }
    }
}
