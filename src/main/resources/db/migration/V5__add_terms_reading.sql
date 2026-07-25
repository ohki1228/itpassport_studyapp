-- 用語集の50音順ソート用の読み仮名(カタカナ)。既存行は空文字で初期化し、直後の再取込みで実データに更新される。
ALTER TABLE terms ADD COLUMN reading VARCHAR(100) NOT NULL DEFAULT '';

CREATE INDEX idx_terms_reading ON terms (reading);
