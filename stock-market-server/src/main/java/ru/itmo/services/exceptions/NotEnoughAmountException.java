package ru.itmo.services.exceptions;

public class NotEnoughAmountException extends Exception {
    public NotEnoughAmountException(String message) {
        super(message);
    }
}
