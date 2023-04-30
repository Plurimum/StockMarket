package ru.itmo.services.exceptions;

public class StocksNotFoundException extends Exception {
    public StocksNotFoundException(String message) {
        super(message);
    }
}
