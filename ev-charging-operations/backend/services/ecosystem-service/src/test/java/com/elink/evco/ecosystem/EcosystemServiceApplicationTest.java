package com.elink.evco.ecosystem;

import com.elink.evco.testkit.ServiceArchitectureAssertions;
import com.elink.evco.ecosystem.bootstrap.EcosystemServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EcosystemServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EcosystemServiceApplicationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesHealthWithoutBusinessEndpoints() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void keepsLayerDependenciesOneWay() {
        ServiceArchitectureAssertions.assertLayerBoundaries("com.elink.evco.ecosystem");
    }
}
