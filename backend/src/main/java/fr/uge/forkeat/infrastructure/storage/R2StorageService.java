package fr.uge.forkeat.infrastructure.storage;

import fr.uge.forkeat.service.exception.ImageUploadException;
import fr.uge.forkeat.service.model.ImageUpload;
import fr.uge.forkeat.service.port.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;
import java.util.function.UnaryOperator;

@Service
public class R2StorageService implements StoragePort {

  private static final Logger logger = LoggerFactory.getLogger(R2StorageService.class);

  private final S3Client s3Client;
  private final String bucketName;
  private final String publicUrl;
  private final long sizeLimit;

  private final UnaryOperator<String> getFileExtension = filename -> {
    if (filename == null || !filename.contains(".")) {
      return ".jpg";
    }
    return filename.substring(filename.lastIndexOf("."));
  };


  public R2StorageService(S3Client s3Client, @Value("${cloudflare.r2.bucket.name}") String bucketName,
                          @Value("${cloudflare.r2.public.url}") String publicUrl, @Value("${cloudflare-r2-limit-size-image}") long sizeLimit) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.publicUrl = publicUrl;
    if (sizeLimit <= 0) throw new IllegalArgumentException("Size limit must be > 0");
    this.sizeLimit = sizeLimit;
  }



  @Override
  public String uploadImage(ImageUpload image, String folder) {
    validateImage(image);
    var extension = getFileExtension.apply(image.originalFilename());
    var key = folder + "/" + UUID.randomUUID() + extension;
    try{
      var putRequest = PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentType(image.contentType())
              .contentLength((long) image.bytes().length)
              .build();
      s3Client.putObject(putRequest, RequestBody.fromBytes(image.bytes()));
      logger.info("Image uploaded to S3 bucket: {}", key);
      return publicUrl + "/" + key;
    }catch(S3Exception e){
      logger.error("Failed to upload image to R2", e);
      throw new ImageUploadException("Failed to upload image", e);
    }
  }

  @Override
  public void deleteImage(String imageUrl) {
    if (!imageUrl.startsWith(publicUrl + "/")) {
      logger.info("deleteImage ignoré : image externe ou hors bucket ({})", imageUrl);
      return;
    }
    try {
      var key = imageUrl.replace(publicUrl + "/", "");
      var deleteRequest = DeleteObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .build();
      s3Client.deleteObject(deleteRequest);
      logger.info("Image deleted successfully from R2: {}", key);
    } catch (S3Exception e) {
      logger.error("Failed to delete image from R2: {}", imageUrl, e);
    }
  }

  private void validateImage(ImageUpload file) {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    var contentType = file.contentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("File must be an image");
    }

    if (file.bytes().length > sizeLimit * 1024 * 1024) {
      throw new IllegalArgumentException("File size must be less than 5 MB");
    }
  }
}
