package ru.itmo.model;

public record ClientSellStockRequest(String id, String companyName, int count) {
}
