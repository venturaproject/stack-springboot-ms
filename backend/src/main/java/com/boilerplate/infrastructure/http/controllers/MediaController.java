package com.boilerplate.infrastructure.http.controllers;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final Path rootDir;

    public MediaController(@Value("${app.media.dir:./media}") String mediaDir) {
        this.rootDir = Paths.get(mediaDir);
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String relativePath = uri.replaceFirst("^/api/media/", "");
        Path filePath = rootDir.resolve(relativePath).normalize();

        if (!filePath.startsWith(rootDir.normalize())) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        Path nameComponent = filePath.getFileName();
        if (nameComponent == null) {
            return ResponseEntity.badRequest().build();
        }
        String filename = nameComponent.toString().toLowerCase(Locale.ROOT);
        MediaType contentType = filename.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : filename.endsWith(".gif") ? MediaType.IMAGE_GIF : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok().contentType(contentType).body(resource);
    }
}
