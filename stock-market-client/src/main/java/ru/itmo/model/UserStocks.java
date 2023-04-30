package ru.itmo.model;

import java.util.Objects;

public class UserStocks {
    private final String companyName;
    private final int cost;

    private volatile int count;

    public UserStocks(int count, String companyName, int cost) {
        this.count = count;
        this.companyName = companyName;
        this.cost = cost;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getCost() {
        return cost;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStocks that = (UserStocks) o;
        return cost == that.cost && count == that.count && Objects.equals(companyName, that.companyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyName, cost, count);
    }

    @Override
    public String toString() {
        return "UserStocks{" +
                "companyName='" + companyName + '\'' +
                ", cost=" + cost +
                ", count=" + count +
                '}';
    }
}
