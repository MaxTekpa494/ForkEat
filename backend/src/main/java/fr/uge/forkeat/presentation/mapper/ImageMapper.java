package fr.uge.forkeat.presentation.mapper;

import fr.uge.forkeat.service.exception.ImageUploadException;
import fr.uge.forkeat.service.model.ImageUpload;
import org.springframework.web.multipart.MultipartFile;

public class ImageMapper {


  private ImageMapper(){}

  public static ImageUpload toImageUpload(MultipartFile file) {
    if (file == null || file.isEmpty()) return null;
    try{
      return new ImageUpload(file.getBytes(), file.getContentType(), file.getOriginalFilename());
    }catch (Exception e){ // EST-CE LE BON ENDROIT ??
      throw new ImageUploadException("Failed to upload image", e);
    }
  }
}
