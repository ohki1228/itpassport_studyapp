-- 用語集詳細ページで、由来する基礎知識単元の本文をそのまま表示するための参照。
-- knowledge.body を複製せず、この外部キー経由で参照することで単一の情報源を保つ。
ALTER TABLE terms ADD COLUMN knowledge_id BIGINT REFERENCES knowledge(id);

CREATE INDEX idx_terms_knowledge ON terms (knowledge_id);
