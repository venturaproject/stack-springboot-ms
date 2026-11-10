package com.boilerplate.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.application.storage.ImageUpload;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void ignores_client_filename_and_reencodes_an_actual_png() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        LocalMediaStorage storage = new LocalMediaStorage(tempDir.toString());
        UUID productId = UUID.randomUUID();

        ImageUpload upload = new ImageUpload(bytes.toByteArray(), "payload.html", "text/html");
        String path = storage.storeProductImage(productId, upload);

        assertThat(path).isEqualTo("/media/products/" + productId + ".png");
        assertThat(ImageIO.read(
                        tempDir.resolve("products/" + productId + ".png").toFile()))
                .isNotNull();
    }

    @Test
    void rejects_non_image_content() {
        LocalMediaStorage storage = new LocalMediaStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.storeAvatar(
                        UUID.randomUUID(),
                        new ImageUpload(
                                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8),
                                "avatar.png",
                                "image/png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }
}
