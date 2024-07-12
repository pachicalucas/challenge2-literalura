package br.com.alura.literalura.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ConsumoAPI {
    private static final Logger logger = LoggerFactory.getLogger(ConsumoAPI.class);

    public String obterDadosAPI(String url) {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logResponseDetails(response);
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new RuntimeException("Failed to fetch data. HTTP Status Code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error fetching data from API: " + e.getMessage(), e);
        }
    }

    private void logResponseDetails(HttpResponse<String> response) {
        logger.info("Status Code: " + response.statusCode());
        logger.info("Response Headers: " + response.headers());
        logger.info("Response Body: " + response.body());
    }
}