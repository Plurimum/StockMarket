package ru.itmo.domain;

public record BuyStockRequest(String companyName, int count, int userAmountMoney) {
}
