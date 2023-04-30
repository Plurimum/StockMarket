package ru.itmo.model;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class User {
    private final String name;
    private final AtomicInteger amount;
    private final ConcurrentLinkedDeque<UserStocks> stocks = new ConcurrentLinkedDeque<>();

    public User(String name, AtomicInteger amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public ConcurrentLinkedDeque<UserStocks> getStocks() {
        return stocks;
    }

    public AtomicInteger getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(name, user.name) && Objects.equals(amount, user.amount) && Objects.equals(stocks, user.stocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, amount, stocks);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", amount=" + amount +
                ", stocks=" + stocks +
                '}';
    }
}
