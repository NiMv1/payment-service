package com.paymentservice.domain.enums;

/**
 * Поддерживаемые валюты.
 */
public enum Currency {
    
    RUB("Российский рубль", "₽"),
    USD("Доллар США", "$"),
    EUR("Евро", "€"),
    GBP("Фунт стерлингов", "£"),
    CNY("Китайский юань", "¥");
    
    private final String name;
    private final String symbol;
    
    Currency(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public String getSymbol() {
        return symbol;
    }
}
