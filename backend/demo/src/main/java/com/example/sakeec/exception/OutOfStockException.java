package com.example.sakeec.exception;

public class OutOfStockException extends BusinessException {

    public OutOfStockException() {
        super("OUT_OF_STOCK", "在庫が不足しています");
    }
}
