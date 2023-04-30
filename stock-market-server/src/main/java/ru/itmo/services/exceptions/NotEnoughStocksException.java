package ru.itmo.services.exceptions;

public class NotEnoughStocksException extends Exception {
    public NotEnoughStocksException(String message) {
        super(message);
    }
}
