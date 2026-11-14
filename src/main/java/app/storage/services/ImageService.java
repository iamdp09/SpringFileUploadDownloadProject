package app.storage.services;

import app.storage.repository.ImageMetadata;
import app.storage.repository.ImageMetadataRepository;
import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

@Service
public class ImageService {
    private final ImageStorageProperties properties;
    private final LocalImageStorageService storageService;
    private final ImageMetadataRepository metadataRepository;

    public ImageService(ImageStorageProperties properties, LocalImageStorageService storageService, ImageMetadataRepository metadataRepository) {
        this.properties = properties;
        this.storageService = storageService;
        this.metadataRepository = metadataRepository;
    }

    public ImageMetadata uploadImage(MultipartFile file, String ownerId) throws IOException {
        validateImage(file);

        String storagePath;
        try (InputStream inputStream = file.getInputStream()) {
            storagePath = storageService.storeFile(inputStream, file.getOriginalFilename());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file", e);
        }

        ImageMetadata metadata = new ImageMetadata(
                file.getOriginalFilename(),
                storagePath,
                file.getContentType(),
                ownerId,
                file.getSize(),
                Instant.now(),
                ObjectId.get()
        );

        return metadataRepository.save(metadata);
    }

    public Resource getImageResource(String imageId) throws IOException {
        ImageMetadata metadata = getImageMetadata(imageId);
        return storageService.getFileResource(metadata.storageFilename());
    }

    public ImageMetadata getImageMetadata(String imageId) throws IOException {
        ObjectId objectId = new ObjectId(imageId);
        return metadataRepository.findById(String.valueOf(objectId)).orElseThrow(
                () -> new FileNotFoundException("File not found.")
        );
    }

        private void validateImage (MultipartFile file){
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            String mineType = file.getContentType();

            if (mineType == null || !properties.allowedMimeTypes().contains(mineType)) {
                throw new IllegalArgumentException("File type not allowed: " + mineType);
            }
        }

}
