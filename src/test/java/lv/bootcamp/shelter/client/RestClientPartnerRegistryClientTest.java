package lv.bootcamp.shelter.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RestClientPartnerRegistryClientTest {

    private MockWebServer mockWebServer;

    private RestClientPartnerRegistryClient restClientPartnerRegistryClient;

    @BeforeEach
    public void initServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        restClientPartnerRegistryClient = new RestClientPartnerRegistryClient(
                RestClient.builder(), mockWebServer.url("/").toString());
    }

    @AfterEach
    public void shutDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void lookup_shouldReturnPartnerRegistryResult() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "registered": true,
                          "partnerShelterName": "Partner Shelter"
                        }
                        """));

        PartnerRegistryResult result = restClientPartnerRegistryClient.lookup("MC123456");

        assertThat(result).isEqualTo(new PartnerRegistryResult(true, "Partner Shelter"));
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/registry/MC123456");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void lookup_shouldThrowWhenPartnerReturnsServerError() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"message": "Partner registry unavailable"}
                        """));

        assertThatThrownBy(() -> restClientPartnerRegistryClient.lookup("MC123456"))
                .isInstanceOf(HttpServerErrorException.InternalServerError.class);

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/registry/MC123456");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }
}
