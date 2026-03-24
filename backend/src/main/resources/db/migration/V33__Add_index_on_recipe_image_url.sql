CREATE INDEX idx_recipes_image_url ON recipes (image_url) WHERE image_url IS NOT NULL;
