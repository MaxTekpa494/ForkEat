package fr.uge.forkeat.service.model;


// Peut-être ByteBuffer ??? Mais je ne pense pas car c'est une vieeille API
public record ImageUpload(byte[] bytes, String contentType, String originalFilename) {
}
