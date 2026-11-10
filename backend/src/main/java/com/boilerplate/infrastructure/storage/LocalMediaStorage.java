package com.boilerplate.infrastructure.storage;

import com.boilerplate.application.storage.ImageUpload;
import com.boilerplate.application.storage.MediaStoragePort;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocalMediaStorage implements MediaStoragePort {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 6_000;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;

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
            SanitizedImage sanitized = sanitize(image.content());
            Path dir = rootDir.resolve(folder);
            Files.createDirectories(dir);
            Files.copy(
                    new ByteArrayInputStream(sanitized.content()),
                    dir.resolve(id + sanitized.extension()),
                    StandardCopyOption.REPLACE_EXISTING);
            return "/media/" + folder + "/" + id + sanitized.extension();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + folder + " image: " + e.getMessage(), e);
        }
    }

    private static SanitizedImage sanitize(byte[] content) {
        if (content.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image exceeds the 5 MiB limit");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only JPEG and PNG images are accepted");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toUpperCase(java.util.Locale.ROOT);
                String outputFormat =
                        switch (format) {
                            case "JPG", "JPEG" -> "jpeg";
                            case "PNG" -> "png";
                            default -> throw new IllegalArgumentException("Only JPEG and PNG images are accepted");
                        };
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1
                        || height < 1
                        || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions exceed the allowed limit");
                }

                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Image data is invalid");
                }
                BufferedImage normalized = outputFormat.equals("jpeg") ? toRgb(decoded) : decoded;
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(normalized, outputFormat, output)) {
                        throw new IllegalArgumentException("Image format cannot be safely re-encoded");
                    }
                    return new SanitizedImage(output.toByteArray(), outputFormat.equals("png") ? ".png" : ".jpg");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Image data is invalid", e);
        }
    }

    private static BufferedImage toRgb(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = target.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static final class SanitizedImage {

        private final byte[] content;
        private final String extension;

        private SanitizedImage(byte[] content, String extension) {
            this.content = content;
            this.extension = extension;
        }

        private byte[] content() {
            return content;
        }

        private String extension() {
            return extension;
        }
    }
}
