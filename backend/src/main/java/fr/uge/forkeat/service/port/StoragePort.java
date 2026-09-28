package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.ImageUpload;

public interface StoragePort {
  String uploadImage(ImageUpload image, String folder);
  void deleteImage(String imageId);
}
