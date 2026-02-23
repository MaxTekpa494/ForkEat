package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.service.exception.ImageUploadException;
import fr.uge.forkeat.service.model.ImageUpload;
import org.springframework.web.multipart.MultipartFile;

public class ImageMapper {


  private ImageMapper(){}

  public static ImageUpload toImageUpload(MultipartFile file) {
    // Notez que c'est voulu de ne pas mettre requireNonNull
    if (file == null || file.isEmpty()) return null;
    try{
      return new ImageUpload(file.getBytes(), file.getContentType(), file.getOriginalFilename());
    }catch (Exception e){
      throw new ImageUploadException("Failed to upload image", e);
    }
  }
}
