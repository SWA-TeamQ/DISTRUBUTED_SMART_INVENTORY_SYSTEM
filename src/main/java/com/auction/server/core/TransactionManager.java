package com.auction.server.core;

import java.sql.Connection;

public class TransactionManager {
    private final Connection connection;

    public TransactionManager(Connection connection) {
        this.connection = connection;
    }

    public interface TransactionCallback<T> {
        T doInTransaction() throws Exception;
    }

    public interface TransactionCallbackVoid {
        void doInTransaction() throws Exception;
    }

    public <T> T execute(TransactionCallback<T> action) throws Exception {
        try {
            connection.setAutoCommit(false);
            try {
                T result = action.doInTransaction();
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Database error during transaction boundaries", e);
        }
    }

    public void executeWithoutResult(TransactionCallbackVoid action) throws Exception {
        execute(() -> {
            action.doInTransaction();
            return null;
        });
    }
}
