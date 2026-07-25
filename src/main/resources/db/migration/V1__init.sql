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

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE study_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    mode                VARCHAR(20) NOT NULL CHECK (mode IN ('PRACTICE', 'WEAKNESS', 'MOCK_EXAM')),
    question_count      INTEGER,
    time_limit_minutes  INTEGER,
    started_at          TIMESTAMP NOT NULL DEFAULT now(),
    finished_at         TIMESTAMP
);

CREATE TABLE answer_history (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES study_sessions(id),
    user_id      BIGINT NOT NULL REFERENCES users(id),
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

CREATE INDEX idx_answer_history_user_genre ON answer_history (user_id, genre_id);
CREATE INDEX idx_answer_history_user_mode ON answer_history (user_id, mode, answered_at);
CREATE INDEX idx_questions_genre_status ON questions (genre_id, status);
CREATE INDEX idx_knowledge_genre ON knowledge (genre_id);

-- 固定マスタ投入(大分類)
INSERT INTO categories (id, name, display_order) VALUES
    (1, 'ストラテジ系', 1),
    (2, 'マネジメント系', 2),
    (3, 'テクノロジ系', 3);
