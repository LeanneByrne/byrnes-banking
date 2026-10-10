package com.leannebyrne.bank.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.leannebyrne.bank.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CorrelationIdFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyResponseCarriesATraceIdHeader() throws Exception {
        String traceId = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getHeader("X-Trace-Id");

        assertThat(traceId).isNotBlank();
    }
}
