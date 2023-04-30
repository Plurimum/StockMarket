package ru.itmo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.domain.BuyStockRequest;
import ru.itmo.domain.SellStockRequest;
import ru.itmo.domain.AddStockRequest;
import ru.itmo.domain.SetCostRequest;
import ru.itmo.domain.Stocks;
import ru.itmo.domain.StocksSnapshot;
import ru.itmo.services.StockMarketService;
import ru.itmo.services.exceptions.NotEnoughAmountException;
import ru.itmo.services.exceptions.NotEnoughStocksException;
import ru.itmo.services.exceptions.StocksNotFoundException;

@RestController
public class StocksMarketController {
    private static Logger LOGGER = LoggerFactory.getLogger(StocksMarketController.class);

    private final StockMarketService stockMarketService;

    public StocksMarketController(StockMarketService stockMarketService) {
        this.stockMarketService = stockMarketService;
    }

    @PostMapping("/add/company")
    public ResponseEntity<Void> addCompany(@RequestBody String companyName) {
        stockMarketService.addCompany(companyName);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/add/stocks")
    public ResponseEntity<Void> addStocks(@RequestBody AddStockRequest addStockRequest) {
        try {
            stockMarketService.addStocks(addStockRequest);

            return ResponseEntity.ok().build();
        } catch (StocksNotFoundException e) {
            LOGGER.error("Stock requested with info {} not found", addStockRequest, e);

            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/set/cost")
    public ResponseEntity<Void> setCost(@RequestBody SetCostRequest setCostRequest) {
        try {
            stockMarketService.setCost(setCostRequest);

            return ResponseEntity.ok().build();
        } catch (StocksNotFoundException e) {
            LOGGER.error("Stock requested with info {} not found", setCostRequest, e);

            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/get/stocks")
    public ResponseEntity<StocksSnapshot> getStocks(@RequestBody String companyName) {
        try {
            Stocks stocks = stockMarketService.getStocks(companyName);

            return ResponseEntity.ok(new StocksSnapshot(stocks.count().get(), stocks.cost().get()));
        } catch (StocksNotFoundException e) {
            LOGGER.error("Stock requested with info {} not found", companyName, e);

            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/buy/stocks")
    public ResponseEntity<StocksSnapshot> buyStocks(@RequestBody BuyStockRequest buyStockRequest) {
        try {
            return ResponseEntity.ok(
                    stockMarketService.buyStocks(buyStockRequest)
            );
        } catch (StocksNotFoundException e) {
            LOGGER.error("Stock requested with info {} not found", buyStockRequest, e);

            return ResponseEntity.notFound().build();
        } catch (NotEnoughAmountException | NotEnoughStocksException e) {
            LOGGER.error("Not enough amount or stocks for request: {}", buyStockRequest, e);

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/sell/stocks")
    public ResponseEntity<Integer> sellStocks(@RequestBody SellStockRequest sellStocksRequest) {
        try {
            return ResponseEntity.ok(stockMarketService.sellStocks(sellStocksRequest));
        } catch (StocksNotFoundException e) {
            LOGGER.error("Stock requested with info {} not found", sellStocksRequest, e);

            return ResponseEntity.notFound().build();
        }
    }
}
