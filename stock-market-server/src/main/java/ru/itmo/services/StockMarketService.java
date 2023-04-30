package ru.itmo.services;

import org.springframework.stereotype.Service;
import ru.itmo.domain.BuyStockRequest;
import ru.itmo.domain.SellStockRequest;
import ru.itmo.domain.StocksSnapshot;
import ru.itmo.domain.AddStockRequest;
import ru.itmo.domain.SetCostRequest;
import ru.itmo.domain.Stocks;
import ru.itmo.services.exceptions.NotEnoughAmountException;
import ru.itmo.services.exceptions.NotEnoughStocksException;
import ru.itmo.services.exceptions.StocksNotFoundException;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StockMarketService {
    private final ConcurrentHashMap<String, Stocks> stocksMarket;

    public StockMarketService() {
        this.stocksMarket = new ConcurrentHashMap<>();
    }

    public void addCompany(String companyName) {
        stocksMarket.putIfAbsent(
                companyName,
                new Stocks(
                        new AtomicInteger(0),
                        new AtomicInteger(0)
                )
        );
    }

    public void addStocks(AddStockRequest addStockRequest) throws StocksNotFoundException {
        final Stocks stock = getStocks(addStockRequest.companyName());

        stock.count().addAndGet(addStockRequest.count());
    }

    public Stocks getStocks(String companyName) throws StocksNotFoundException {
        final Optional<Stocks> optionalStocks = Optional.ofNullable(stocksMarket.get(companyName));

        return optionalStocks.orElseThrow(
                () -> new StocksNotFoundException("Can't find stocks for company name: " + companyName)
        );
    }

    public void setCost(SetCostRequest setCostRequest) throws StocksNotFoundException {
        final Stocks stock = getStocks(setCostRequest.companyName());

        stock.cost().set(setCostRequest.cost());
    }

    public StocksSnapshot buyStocks(
            BuyStockRequest buyStockRequest
    ) throws StocksNotFoundException, NotEnoughAmountException, NotEnoughStocksException {
        while (true) {
            final Stocks stock = getStocks(buyStockRequest.companyName());
            final int stockCount = stock.count().get();
            final int stockCost = stock.cost().get();

            if (buyStockRequest.count() * stockCost > buyStockRequest.userAmountMoney()) {
                final String errorMessage = String.format(
                        "Not enough amount on user balance for buy stocks " +
                                "of company '%s'. %nUser amount: %d%nRequired amount: %d",
                        buyStockRequest.companyName(),
                        buyStockRequest.userAmountMoney(),
                        buyStockRequest.count() * stockCost
                );

                throw new NotEnoughAmountException(errorMessage);
            }

            if (stockCount < buyStockRequest.count()) {
                final String errorMessage = String.format(
                        "Not enough stocks on StocksMarket of company: %s%n" +
                                "Count of stocks requested by the user: %d%nCount of stocks on StocksMarket: %d",
                        buyStockRequest.companyName(),
                        buyStockRequest.count(),
                        stockCount
                );

                throw new NotEnoughStocksException(errorMessage);
            }

            if (stock.cost().compareAndSet(stockCost, stockCost) &&
                    stock.count().compareAndSet(stockCount, stockCount - buyStockRequest.count())
            ) {
                return new StocksSnapshot(
                        buyStockRequest.count(),
                        stockCost
                );
            }

        }
    }

    public int sellStocks(SellStockRequest sellStocksRequest) throws StocksNotFoundException {
        final Stocks stock = getStocks(sellStocksRequest.companyName());
        final int stockCost = stock.cost().get();

        stock.count().addAndGet(sellStocksRequest.count());

        return stockCost * sellStocksRequest.count();
    }
}
