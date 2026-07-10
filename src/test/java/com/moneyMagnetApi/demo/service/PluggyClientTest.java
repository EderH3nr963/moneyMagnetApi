package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyConnectTokenResponse;
import com.moneyMagnetApi.demo.dto.pluggy.response.PluggyItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PluggyClientTest {

    private MockRestServiceServer server;
    private PluggyClient pluggyClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.pluggy.ai");

        server = MockRestServiceServer.bindTo(builder).build();
        pluggyClient = new PluggyClient(builder.build());
        ReflectionTestUtils.setField(pluggyClient, "configuredApiKey", "api-key");
    }

    @Test
    void shouldCreateConnectTokenForAuthenticatedUser() {
        UUID usuarioId = UUID.fromString("0abcb755-726b-48df-939a-4b4b8146509d");

        server.expect(requestTo("https://api.pluggy.ai/connect_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "api-key"))
                .andExpect(content().json("""
                        {
                          "options": {
                            "clientUserId": "0abcb755-726b-48df-939a-4b4b8146509d",
                            "avoidDuplicates": true
                          }
                        }
                        """))
                .andRespond(withSuccess(
                        "{\"accessToken\":\"connect-token\"}",
                        MediaType.APPLICATION_JSON
                ));

        PluggyConnectTokenResponse response = pluggyClient.createConnectToken(usuarioId);

        assertThat(response.accessToken()).isEqualTo("connect-token");
        server.verify();
    }

    @Test
    void shouldRetrieveItemBeforePersistingItLocally() {
        String itemId = "21d4c225-c4ff-42d2-89bf-f0623994c363";

        server.expect(requestTo("https://api.pluggy.ai/items/" + itemId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "api-key"))
                .andRespond(withSuccess("""
                        {
                          "id": "21d4c225-c4ff-42d2-89bf-f0623994c363",
                          "connector": {
                            "id": 201,
                            "name": "Banco Teste",
                            "imageUrl": "https://cdn.pluggy.ai/banco.svg",
                            "primaryColor": "#123456"
                          },
                          "status": "UPDATED",
                          "executionStatus": "SUCCESS",
                          "clientUserId": "3de57ed9-6174-47f9-bb54-c07fd0d1b3c2"
                        }
                        """, MediaType.APPLICATION_JSON));

        PluggyItemResponse response = pluggyClient.getItem(itemId);

        assertThat(response.id()).isEqualTo(itemId);
        assertThat(response.connector().id()).isEqualTo(201L);
        assertThat(response.executionStatus()).isEqualTo("SUCCESS");
        server.verify();
    }
}
