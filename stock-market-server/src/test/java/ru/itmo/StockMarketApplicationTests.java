package ru.itmo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.itmo.domain.BuyStockRequest;
import ru.itmo.domain.StocksSnapshot;
import ru.itmo.domain.AddStockRequest;
import ru.itmo.domain.SetCostRequest;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StockMarketApplicationTests {
    private static final String COMPANY_NAME = "VK Company";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testFullCycle() throws UnsupportedEncodingException, JsonProcessingException {
        postRaw("/add/company", COMPANY_NAME);
        post("/add/stocks", new AddStockRequest(COMPANY_NAME, 1000));
        post("/set/cost", new SetCostRequest(COMPANY_NAME, 100));

        final String buyStocksResult = getPostResult(
                "/buy/stocks",
                new BuyStockRequest(COMPANY_NAME, 5, 1000)
        );
        final StocksSnapshot stocksSnapshot = objectMapper.readValue(buyStocksResult, StocksSnapshot.class);

        assertEquals(5, stocksSnapshot.count());
        assertEquals(100, stocksSnapshot.cost());

        post("/set/cost", new SetCostRequest(COMPANY_NAME, 200));

        final String getStocksResult = getRawPostResult("/get/stocks", COMPANY_NAME);
        final StocksSnapshot stocksSnapshotNewCost = objectMapper.readValue(getStocksResult, StocksSnapshot.class);

        assertEquals(200, stocksSnapshotNewCost.cost());
        assertEquals(995, stocksSnapshotNewCost.count());
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
}
