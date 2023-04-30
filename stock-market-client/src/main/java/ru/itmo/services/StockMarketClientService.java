package ru.itmo.services;

import org.springframework.stereotype.Service;
import ru.itmo.client.StockMarketClient;
import ru.itmo.domain.StocksSnapshot;
import ru.itmo.model.ClientBuyStockRequest;
import ru.itmo.model.ClientSellStockRequest;
import ru.itmo.model.DepositRequest;
import ru.itmo.model.User;
import ru.itmo.model.UserStocks;
import ru.itmo.services.exceptions.NotEnoughStocksException;
import ru.itmo.services.exceptions.UserNotFoundException;
import ru.itmo.services.exceptions.UserStocksNotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StockMarketClientService {
    private final StockMarketClient stockMarketClient;
    private final ConcurrentHashMap<String, User> userRepository;

    public StockMarketClientService(StockMarketClient stockMarketClient) {
        this.stockMarketClient = stockMarketClient;
        this.userRepository = new ConcurrentHashMap<>();
    }

    public String registerUser(String name) {
        String id = UUID.randomUUID().toString();

        userRepository.put(id, new User(name, new AtomicInteger(0)));

        return id;
    }

    public void addMoney(DepositRequest depositRequest) throws UserNotFoundException {
        User user = getUser(depositRequest.id());

        user.getAmount().addAndGet(depositRequest.amount());
    }

    public User getUser(String id) throws UserNotFoundException {
        User user = userRepository.get(id);

        return Optional.ofNullable(user).orElseThrow(() -> new UserNotFoundException("Can't find user with id: " + id));
    }

    public int getUserMoney(String id) throws UserNotFoundException {
        User user = getUser(id);

        return user.getAmount().get() +
                getUserStocks(id).stream()
                        .map(userStocks -> userStocks.getCount() * userStocks.getCost())
                        .reduce(Integer::sum)
                        .orElse(0);
    }

    public List<UserStocks> getUserStocks(String id) throws UserNotFoundException {
        return getUser(id).getStocks()
                .stream().map(userStocks -> {
                    StocksSnapshot stocksSnapshot = stockMarketClient.getStock(userStocks.getCompanyName());

                    return new UserStocks(userStocks.getCount(), userStocks.getCompanyName(), stocksSnapshot.cost());
                })
                .toList();

    }

    public void buyStocks(ClientBuyStockRequest clientBuyStockRequest) throws UserNotFoundException {
        User user = getUser(clientBuyStockRequest.id());
        int userMoney = user.getAmount().get();

        StocksSnapshot stocksSnapshot = stockMarketClient.buyStock(
                clientBuyStockRequest.companyName(),
                clientBuyStockRequest.count(),
                userMoney
        );

        user.getAmount().addAndGet(-stocksSnapshot.cost() * stocksSnapshot.count());
        user.getStocks().add(
                new UserStocks(
                        stocksSnapshot.count(),
                        clientBuyStockRequest.companyName(),
                        stocksSnapshot.cost()
                )
        );
    }

    public void sellStock(
            ClientSellStockRequest clientSellStockRequest
    ) throws UserNotFoundException, UserStocksNotFoundException, NotEnoughStocksException {
        User user = getUser(clientSellStockRequest.id());
        UserStocks userStocks = user.getStocks().stream()
                .filter(us -> Objects.equals(us.getCompanyName(), clientSellStockRequest.companyName()))
                .findAny()
                .orElseThrow(() ->
                        new UserStocksNotFoundException(String.format(
                                "Can't find '%s' in portfolio of user with id: %s",
                                clientSellStockRequest.companyName(),
                                clientSellStockRequest.id()
                        ))
                );

        if (userStocks.getCount() < clientSellStockRequest.count()) {
            throw new NotEnoughStocksException(
                    String.format(
                            "Not enough stocks of company '%s' in portfolio of user with id: %s%n" +
                                    "User sell request count: %d%nUsers count of these stocks: %d",
                            clientSellStockRequest.companyName(),
                            clientSellStockRequest.id(),
                            clientSellStockRequest.count(),
                            userStocks.getCount()
                    )
            );
        }

        int amount = stockMarketClient.sellStocks(userStocks.getCompanyName(), clientSellStockRequest.count());

        user.getAmount().addAndGet(amount);
        userStocks.setCount(userStocks.getCount() - clientSellStockRequest.count());

        if (userStocks.getCount() == 0) {
            user.getStocks().remove(userStocks);
        }
    }
}
