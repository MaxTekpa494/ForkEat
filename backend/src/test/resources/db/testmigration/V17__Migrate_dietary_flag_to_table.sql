-- V17 : Migration dietary_flag (JSONB) vers tables normalisées

-- Table des régimes alimentaires
CREATE TABLE "dietary" (
    "id"   UUID         NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("name")
);

CREATE TABLE "recipe_dietary" (
    "id"         UUID NOT NULL,
    "recipe_id"  UUID NOT NULL,
    "dietary_id" UUID NOT NULL,
    PRIMARY KEY ("id"),
    UNIQUE ("recipe_id", "dietary_id"),
    FOREIGN KEY ("recipe_id")  REFERENCES "recipes"("id") ON DELETE CASCADE,
    FOREIGN KEY ("dietary_id") REFERENCES "dietary"("id")
);

INSERT INTO "dietary" ("id", "name")
SELECT gen_random_uuid(), key
FROM (
    SELECT DISTINCT key
    FROM "recipes"
    CROSS JOIN LATERAL jsonb_object_keys("dietary_flag") AS key
    WHERE "dietary_flag" IS NOT NULL
) AS distinct_keys;

INSERT INTO "recipe_dietary" ("id", "recipe_id", "dietary_id")
SELECT gen_random_uuid(), r."id", d."id"
FROM "recipes" r
CROSS JOIN LATERAL jsonb_each(r."dietary_flag") AS flags(key, value)
JOIN "dietary" d ON d."name" = flags.key
WHERE flags.value = 'true'::jsonb;

ALTER TABLE "recipes" DROP COLUMN "dietary_flag";
