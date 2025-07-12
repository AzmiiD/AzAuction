package io.github.AzmiiD.azAuction.models;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionItem {

    private int id;
    private UUID sellerId;
    private String sellerName;
    private ItemStack item;
    private double price;
    private long listTime;
    private long expireTime;

    public AuctionItem(int id, UUID sellerId, String sellerName, ItemStack item, double price, long listTime, long expireTime) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.listTime = listTime;
        this.expireTime = expireTime;
    }

    public AuctionItem(UUID sellerId, String sellerName, ItemStack item, double price, long listTime, long expireTime) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.listTime = listTime;
        this.expireTime = expireTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getListTime() {
        return listTime;
    }

    public void setListTime(long listTime) {
        this.listTime = listTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}