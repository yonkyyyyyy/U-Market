package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.storage.StoredObject;

public record ProcessedImage(StoredObject thumb, StoredObject medium, StoredObject full) {

    public String thumbUrl() {
        return thumb.url();
    }

    public String mediumUrl() {
        return medium.url();
    }

    public String fullUrl() {
        return full.url();
    }
}