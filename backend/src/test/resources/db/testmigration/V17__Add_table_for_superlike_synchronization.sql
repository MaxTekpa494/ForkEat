CREATE TABLE super_likes (
         "id" UUID PRIMARY KEY,
        "user_id" UUID NOT NULL,
        "recipe_id" UUID NOT NULL,
         "amount" BIGINT NOT NULL,
        CONSTRAINT fk_superlike_user
            FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE,
        CONSTRAINT fk_superlike_recipe
            FOREIGN KEY ("recipe_id") REFERENCES "recipes"("id") ON DELETE CASCADE
);