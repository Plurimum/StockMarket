package ru.itmo.services.exceptions;

public class UserStocksNotFoundException extends Exception {
    public UserStocksNotFoundException(String message) {
        super(message);
    }
}
