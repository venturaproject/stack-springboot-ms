package com.boilerplate.infrastructure.storage;

import com.boilerplate.application.storage.ImageUpload;
import com.boilerplate.application.storage.MediaStoragePort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocalMediaStorage implements MediaStoragePort {

    private final Path rootDir;

    public LocalMediaStorage(@Value("${app.media.dir:./media}") String mediaDir) {
        this.rootDir = Paths.get(mediaDir);
    }

    @Override
    public String storeAvatar(UUID userId, ImageUpload image) {
        return store("avatars", userId, image);
    }

    @Override
    public String storeProductImage(UUID productId, ImageUpload image) {
        return store("products", productId, image);
    }

    @Override
    public void delete(String publicPath) {
        try {
            String relative = publicPath.replaceFirst("^/media/", "");
            Path target = rootDir.resolve(relative).normalize();
            if (target.startsWith(rootDir.normalize())) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete media file {}: {}", publicPath, e.getMessage());
        }
    }

    private String store(String folder, UUID id, ImageUpload image) {
        try {
            String original = image.originalFilename();
            String ext =
                    original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
            Path dir = rootDir.resolve(folder);
            Files.createDirectories(dir);
            Files.copy(
                    new ByteArrayInputStream(image.content()),
                    dir.resolve(id + ext),
                    StandardCopyOption.REPLACE_EXISTING);
            return "/media/" + folder + "/" + id + ext;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + folder + " image: " + e.getMessage(), e);
        }
    }
}
