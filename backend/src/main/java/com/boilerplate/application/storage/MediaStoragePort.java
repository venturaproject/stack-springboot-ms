package com.boilerplate.application.storage;

import java.util.UUID;

/** Outbound port for binary media. Local-filesystem adapter: {@code infrastructure.storage.LocalMediaStorage}. */
public interface MediaStoragePort {

    /** Stores the user's avatar and returns the public URL path to it. */
    String storeAvatar(UUID userId, ImageUpload image);

    /** Stores a product image and returns the public URL path to it. */
    String storeProductImage(UUID productId, ImageUpload image);

    /** Removes a previously stored file given the public URL path returned by a {@code store*} call. */
    void delete(String publicPath);
}
