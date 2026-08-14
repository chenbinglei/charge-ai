package com.elink.evco.finance;

import com.elink.evco.finance.FinanceServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FinanceServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinanceServiceApplicationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesHealthWithoutBusinessEndpoints() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }

}
