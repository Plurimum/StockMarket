package ru.itmo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import ru.itmo.domain.AddStockRequest;
import ru.itmo.domain.SetCostRequest;
import ru.itmo.model.ClientBuyStockRequest;
import ru.itmo.model.ClientSellStockRequest;
import ru.itmo.model.DepositRequest;
import ru.itmo.model.UserStocks;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class StockMarketClientTest {
    private static class MyContainer extends FixedHostPortGenericContainer<MyContainer> {
        public MyContainer(@NotNull String dockerImageName) {
            super(dockerImageName);
        }
    }

    private final MyContainer container = new MyContainer("ru.itmo/stock-market-server:0.0.1-snapshot")
            .withFixedExposedPort(8080, 8080)
            .withExposedPorts(8080);
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders headers = new HttpHeaders();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userId;

    private static final String COMPANY_NAME = "VK Company";

    @BeforeEach
    public void prepare() throws JsonProcessingException, UnsupportedEncodingException {
        headers.setContentType(MediaType.APPLICATION_JSON);
        container.start();

        restTemplate.exchange(
                "http://localhost:8080/add/company",
                HttpMethod.POST,
                new HttpEntity<>(COMPANY_NAME, headers),
                Void.class
        );

        restTemplate.exchange(
                "http://localhost:8080/add/stocks",
                HttpMethod.POST,
                new HttpEntity<>(
                        objectMapper.writeValueAsString(new AddStockRequest(COMPANY_NAME, 200)),
                        headers
                ),
                Void.class
        );

        restTemplate.exchange(
                "http://localhost:8080/set/cost",
                HttpMethod.POST,
                new HttpEntity<>(
                        objectMapper.writeValueAsString(new SetCostRequest(COMPANY_NAME, 100)),
                        headers
                        ),
                Void.class
        );

        userId = getRawPostResult("/register", "Maxim Likhanov");
        post("/add/money", new DepositRequest(userId, 1000));
    }

    @AfterEach
    public void cleanUp() {
        container.stop();
    }

    @Test
    public void testNotFoundUser() throws Exception {
        getRaw("/get/user/money", UUID.randomUUID().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBuyStocksThenCostUp() throws Exception {
        post("/buy/stocks", new ClientBuyStockRequest(userId, COMPANY_NAME, 5));

        restTemplate.exchange(
                "http://localhost:8080/set/cost",
                HttpMethod.POST,
                new HttpEntity<>(
                        objectMapper.writeValueAsString(new SetCostRequest(COMPANY_NAME, 300)),
                        headers
                        ),
                Void.class
        );

        final int userBalanceBefore = Integer.parseInt(
                getRawResult("/get/user/money", userId)
        );

        assertEquals(2000, userBalanceBefore);

        final String expectedUserStocksString = objectMapper.writeValueAsString(
                List.of(
                        new UserStocks(
                                5,
                                COMPANY_NAME,
                                300
                        )
                )
        );
        final String actualUserStocksString = getRawResult("/get/stocks", userId);

        assertEquals(expectedUserStocksString, actualUserStocksString);
    }

    @Test
    public void testBuyAndSell() throws Exception {
        post("/buy/stocks", new ClientBuyStockRequest(userId, COMPANY_NAME, 5));

        final int actualUserBalance = Integer.parseInt(getRawResult("/get/user/money", userId));

        assertEquals(1000, actualUserBalance);

        final String expectedUserStocksString = objectMapper.writeValueAsString(
                List.of(
                        new UserStocks(
                                5,
                                COMPANY_NAME,
                                100
                        )
                )
        );
        final String actualUserStocksString = getRawResult("/get/stocks", userId);

        assertEquals(expectedUserStocksString, actualUserStocksString);

        post("/sell/stocks", new ClientSellStockRequest(userId, COMPANY_NAME, 5));

        final String expectedStocksAfterSell = objectMapper.writeValueAsString(Collections.emptyList());
        final String actualStocksAfterSell = getRawResult("/get/stocks", userId);

        assertEquals(expectedStocksAfterSell, actualStocksAfterSell);
    }

    private ResultActions post(String path, Object serializableContent) {
        try {
            final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(serializableContent));

            return mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unlucky", e);
        } catch (Exception e) {
            throw new RuntimeException("mega unlucky", e);
        }
    }

    private ResultActions postRaw(String path, String content) {
        try {
            final RequestBuilder requestBuilder = MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content);

            return mockMvc.perform(requestBuilder)
                    .andExpect(status().isOk());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unlucky", e);
        } catch (Exception e) {
            throw new RuntimeException("mega unlucky", e);
        }
    }

    private String getPostResult(String path, Object serializableContent) throws UnsupportedEncodingException {
        return post(path, serializableContent)
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String getRawPostResult(String path, String content) throws UnsupportedEncodingException {
        return postRaw(path, content)
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private ResultActions getRaw(String path, String content) {
        try {
            final RequestBuilder requestBuilder = MockMvcRequestBuilders.get(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content);

            return mockMvc.perform(requestBuilder);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unlucky", e);
        } catch (Exception e) {
            throw new RuntimeException("mega unlucky", e);
        }
    }

    private String getRawResult(String path, String content) throws Exception {
        return getRaw(path, content)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
