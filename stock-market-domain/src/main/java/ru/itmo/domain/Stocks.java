package ru.itmo.domain;

import java.util.concurrent.atomic.AtomicInteger;

public record Stocks(AtomicInteger count, AtomicInteger cost) {
}
