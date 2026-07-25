-- パスワードリセット用トークン。REGISTEREDユーザーのみ対象(ON DELETE CASCADEでユーザー削除時に一緒に消える)。
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
