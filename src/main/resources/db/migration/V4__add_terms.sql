-- 用語集。基礎知識(knowledge)の重要語句をリンク化するための用語辞書。
CREATE TABLE terms (
    id          BIGINT PRIMARY KEY,
    genre_id    BIGINT NOT NULL REFERENCES genres(id),
    term        VARCHAR(100) NOT NULL,
    definition  TEXT NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_terms_term ON terms (term);
CREATE INDEX idx_terms_genre ON terms (genre_id);
