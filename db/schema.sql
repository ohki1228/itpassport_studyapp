-- ITパスポート学習アプリ DBスキーマ(PostgreSQL)
-- 詳細な設計方針は docs/db-design.md を参照

CREATE TABLE categories (
    id             BIGINT PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    display_order  INTEGER NOT NULL
);

CREATE TABLE genres (
    id             BIGINT PRIMARY KEY,
    category_id    BIGINT NOT NULL REFERENCES categories(id),
    name           VARCHAR(100) NOT NULL,
    display_order  INTEGER NOT NULL
);

CREATE TABLE knowledge (
    id          BIGINT PRIMARY KEY,
    genre_id    BIGINT NOT NULL REFERENCES genres(id),
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    keywords    TEXT,
    point       TEXT,
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE questions (
    id                     BIGINT PRIMARY KEY,
    category_id            BIGINT NOT NULL REFERENCES categories(id),
    genre_id               BIGINT NOT NULL REFERENCES genres(id),
    question_text          TEXT NOT NULL,
    answer_type            VARCHAR(10) NOT NULL CHECK (answer_type IN ('SINGLE', 'MULTIPLE')),
    required_answer_count  INTEGER NOT NULL DEFAULT 1,
    explanation            TEXT NOT NULL,
    status                 VARCHAR(10) NOT NULL DEFAULT 'PRIVATE' CHECK (status IN ('PUBLIC', 'PRIVATE')),
    source                 VARCHAR(255),
    table_data             TEXT,
    updated_at             TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE question_choices (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    choice_number  INTEGER NOT NULL CHECK (choice_number BETWEEN 1 AND 4),
    content        TEXT NOT NULL,
    is_correct     BOOLEAN NOT NULL DEFAULT FALSE,
    explanation    TEXT NOT NULL,
    UNIQUE (question_id, choice_number)
);

-- REGISTERED: email/password_hash必須・guest_tokenはNULL。GUEST: guest_token必須・email/password_hashはNULL。
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    user_type       VARCHAR(10) NOT NULL CHECK (user_type IN ('GUEST', 'REGISTERED')),
    email           VARCHAR(255) UNIQUE,
    password_hash   VARCHAR(255),
    guest_token     UUID UNIQUE,
    display_name    VARCHAR(100),
    last_active_at  TIMESTAMP NOT NULL DEFAULT now(),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_identity_by_type CHECK (
        (user_type = 'REGISTERED' AND email IS NOT NULL AND password_hash IS NOT NULL AND guest_token IS NULL)
        OR
        (user_type = 'GUEST' AND guest_token IS NOT NULL AND email IS NULL AND password_hash IS NULL)
    )
);

CREATE TABLE study_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode                VARCHAR(20) NOT NULL CHECK (mode IN ('PRACTICE', 'WEAKNESS', 'MOCK_EXAM')),
    question_count      INTEGER,
    time_limit_minutes  INTEGER,
    started_at          TIMESTAMP NOT NULL DEFAULT now(),
    finished_at         TIMESTAMP
);

CREATE TABLE answer_history (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES study_sessions(id),
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES questions(id),
    genre_id     BIGINT NOT NULL REFERENCES genres(id),
    is_correct   BOOLEAN NOT NULL,
    mode         VARCHAR(20) NOT NULL CHECK (mode IN ('PRACTICE', 'WEAKNESS', 'MOCK_EXAM')),
    answered_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE answer_history_selections (
    answer_history_id  BIGINT NOT NULL REFERENCES answer_history(id) ON DELETE CASCADE,
    choice_number       INTEGER NOT NULL,
    PRIMARY KEY (answer_history_id, choice_number)
);

-- パスワードリセット用トークン。REGISTEREDユーザーのみ対象。
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- 用語集。基礎知識(knowledge)の重要語句をリンク化するための用語辞書。
-- knowledge_idは由来する基礎知識単元への参照(詳細ページでbodyをそのまま表示し、内容の複製を避けるため)。
CREATE TABLE terms (
    id             BIGINT PRIMARY KEY,
    genre_id       BIGINT NOT NULL REFERENCES genres(id),
    knowledge_id   BIGINT REFERENCES knowledge(id),
    term           VARCHAR(100) NOT NULL,
    reading        VARCHAR(100) NOT NULL DEFAULT '',
    definition     TEXT NOT NULL,
    breakdown      TEXT,
    related_terms  TEXT,
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_answer_history_user_genre ON answer_history (user_id, genre_id);
CREATE INDEX idx_answer_history_user_mode ON answer_history (user_id, mode, answered_at);
CREATE INDEX idx_questions_genre_status ON questions (genre_id, status);
CREATE INDEX idx_knowledge_genre ON knowledge (genre_id);
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE UNIQUE INDEX idx_terms_term ON terms (term);
CREATE INDEX idx_terms_genre ON terms (genre_id);
CREATE INDEX idx_terms_reading ON terms (reading);
CREATE INDEX idx_terms_knowledge ON terms (knowledge_id);

-- 固定マスタ投入(大分類)
INSERT INTO categories (id, name, display_order) VALUES
    (1, 'ストラテジ系', 1),
    (2, 'マネジメント系', 2),
    (3, 'テクノロジ系', 3);
