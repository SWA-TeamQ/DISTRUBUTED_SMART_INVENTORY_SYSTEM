package com.inventory.shared.models;

import java.io.Serializable;

public class Product implements Serializable, Comparable<Product> {
    private int id;
    private String name;
    private String sku;
    private double price;
    private int quantity;

    public Product() {}

    public Product(int id, String name, String sku, double price, int quantity) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public int compareTo(Product other) {
        return Integer.compare(this.id, other.id);
    }

    // getters/setters omitted for brevity
}
