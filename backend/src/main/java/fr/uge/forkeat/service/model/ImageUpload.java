package fr.uge.forkeat.service.model;

public record ImageUpload(byte[] bytes, String contentType, String originalFilename) {
}
