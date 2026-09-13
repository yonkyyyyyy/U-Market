package com.uMarket.uMarket.storage;

public record StoredObject(String id, String originalName, String contentType, long size, String url) {
}