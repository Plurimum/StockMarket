package ru.itmo.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.itmo.client.exceptions.StockMarketServerException;
import ru.itmo.domain.BuyStockRequest;
import ru.itmo.domain.SellStockRequest;
import ru.itmo.domain.StocksSnapshot;

import java.util.Optional;

@Component
public class StockMarketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockMarketClient.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;

    @Value("${client.url}")
    private String url;

    public StockMarketClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.headers = new HttpHeaders();

        this.headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public StocksSnapshot getStock(String companyName) {
        return restTemplate.exchange(
                getUrl("/get/stocks"),
                HttpMethod.POST,
                new HttpEntity<>(companyName, headers),
                StocksSnapshot.class
            ).getBody();
    }

    private String getUrl(String path) {
        return String.format("%s%s", url, path);
    }

    public int sellStocks(String companyName, int count) {
        try {
            Integer result = restTemplate.exchange(
                    getUrl("/sell/stocks"),
                    HttpMethod.POST,
                    new HttpEntity<>(
                            objectMapper.writeValueAsString(new SellStockRequest(companyName, count)),
                            headers
                    ),
                    Integer.class
                ).getBody();

            return Optional.ofNullable(result).orElseThrow(StockMarketServerException::new);
        } catch (JsonProcessingException | StockMarketServerException e) {
            LOGGER.error("Can't sell '{}' stocks of company '{}'", count, companyName, e);

            return 0;
        }
    }

    public StocksSnapshot buyStock(String companyName, int count, int userMoney) {
        try {
            return restTemplate.exchange(
                            getUrl("/buy/stocks"),
                            HttpMethod.POST,
                            new HttpEntity<>(
                                    objectMapper.writeValueAsString(new BuyStockRequest(companyName, count, userMoney)),
                                    headers
                                    ),
                            StocksSnapshot.class
                ).getBody();
        } catch (JsonProcessingException e) {
            LOGGER.error("Can't buy '{}' stocks of company '{}'", count, companyName, e);

            return new StocksSnapshot(0, 0);
        }
    }
}
