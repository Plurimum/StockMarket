package ru.itmo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.model.ClientBuyStockRequest;
import ru.itmo.model.ClientSellStockRequest;
import ru.itmo.model.DepositRequest;
import ru.itmo.model.UserStocks;
import ru.itmo.services.StockMarketClientService;
import ru.itmo.services.exceptions.NotEnoughStocksException;
import ru.itmo.services.exceptions.UserNotFoundException;
import ru.itmo.services.exceptions.UserStocksNotFoundException;

import java.util.List;

@RestController
public class StockMarketClientController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockMarketClientController.class);

    private final StockMarketClientService stockMarketClientService;

    public StockMarketClientController(StockMarketClientService stockMarketClientService) {
        this.stockMarketClientService = stockMarketClientService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> onRegister(@RequestBody String name) {
        return ResponseEntity.ok(stockMarketClientService.registerUser(name));
    }

    @PostMapping("/add/money")
    public ResponseEntity<Void> onAddMoney(@RequestBody DepositRequest depositRequest) {
        try {
            stockMarketClientService.addMoney(depositRequest);

            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            LOGGER.error("Can't deposit", e);

            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/get/stocks")
    public ResponseEntity<List<UserStocks>> onGetStocks(@RequestBody String id) {
        try {
            return ResponseEntity.ok(stockMarketClientService.getUserStocks(id));
        } catch (UserNotFoundException e) {
            LOGGER.error("Can't get stocks", e);

            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/get/user/money")
    public ResponseEntity<Integer> onGetUserMoney(@RequestBody String id) {
        try {
            return ResponseEntity.ok(stockMarketClientService.getUserMoney(id));
        } catch (UserNotFoundException e) {
            LOGGER.error("Can't get users money", e);

            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/buy/stocks")
    public ResponseEntity<Void> onBuyStocks(@RequestBody ClientBuyStockRequest clientBuyStockRequest) {
        try {
            stockMarketClientService.buyStocks(clientBuyStockRequest);

            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            LOGGER.error("Can't buy stocks", e);

            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/sell/stocks")
    public ResponseEntity<Void> onSellStocks(@RequestBody ClientSellStockRequest clientSellStockRequest) {
        try {
            stockMarketClientService.sellStock(clientSellStockRequest);

            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            LOGGER.error("Can't sell stocks", e);

            return ResponseEntity.notFound().build();
        } catch (UserStocksNotFoundException | NotEnoughStocksException e) {
            LOGGER.error("Can't sell stocks", e);

            return ResponseEntity.badRequest().build();
        }
    }
}
