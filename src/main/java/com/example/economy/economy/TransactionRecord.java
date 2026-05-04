package com.example.economy.economy;

public record TransactionRecord(
        long id,
        String actorUuid,
        String targetUuid,
        TransactionType type,
        double amount,
        String metaJson,
        long createdAt) {
}
