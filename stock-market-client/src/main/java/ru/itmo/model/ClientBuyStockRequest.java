package ru.itmo.model;

public record ClientBuyStockRequest(String id, String companyName, int count) {
}
