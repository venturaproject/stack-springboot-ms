package com.boilerplate.application.storage;

import jakarta.annotation.Nullable;

/**
 * A framework-neutral uploaded image. Controllers adapt the incoming
 * {@code MultipartFile} into this so the application layer and the storage port never see
 * a servlet/web type.
 */
@SuppressWarnings("ArrayRecordComponent") // small transfer object, copied defensively below
public record ImageUpload(byte[] content, @Nullable String originalFilename, @Nullable String contentType) {

    public ImageUpload {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
