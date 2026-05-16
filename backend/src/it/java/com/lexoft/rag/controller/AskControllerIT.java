package com.lexoft.rag.controller;

import com.lexoft.rag.model.AskRequest;
import com.lexoft.rag.model.AskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AskControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void askEndpoint_returnsAnswerWithEvaluationFields() {
        var request = new AskRequest("Why is the sky blue?");

        ResponseEntity<AskResponse> response = restTemplate.postForEntity("/ask", request, AskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().answer()).isNotBlank();
        assertThat(response.getBody().feedback()).isNotNull();
    }
}
