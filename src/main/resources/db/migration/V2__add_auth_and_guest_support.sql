-- ユーザー認証(email/password)とゲスト利用(UUIDトークン)の両対応。

ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL,
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users ADD COLUMN user_type VARCHAR(10) NOT NULL DEFAULT 'REGISTERED';
ALTER TABLE users ADD COLUMN guest_token UUID;
ALTER TABLE users ADD COLUMN last_active_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE users ALTER COLUMN user_type DROP DEFAULT;

ALTER TABLE users ADD CONSTRAINT chk_users_user_type CHECK (user_type IN ('GUEST', 'REGISTERED'));
ALTER TABLE users ADD CONSTRAINT uq_users_guest_token UNIQUE (guest_token);

-- REGISTEREDはemail/password_hash必須かつguest_tokenを持たない。GUESTはその逆。
ALTER TABLE users ADD CONSTRAINT chk_users_identity_by_type CHECK (
    (user_type = 'REGISTERED' AND email IS NOT NULL AND password_hash IS NOT NULL AND guest_token IS NULL)
    OR
    (user_type = 'GUEST' AND guest_token IS NOT NULL AND email IS NULL AND password_hash IS NULL)
);

-- ゲストアカウントの保持期限切れ削除(定期ジョブ)で履歴も一緒に消えるようにする。
ALTER TABLE study_sessions DROP CONSTRAINT study_sessions_user_id_fkey;
ALTER TABLE study_sessions ADD CONSTRAINT study_sessions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE answer_history DROP CONSTRAINT answer_history_user_id_fkey;
ALTER TABLE answer_history ADD CONSTRAINT answer_history_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 認証実装までの暫定デモユーザーは不要になったため削除(履歴もCASCADEで削除される)。
DELETE FROM users WHERE email = 'demo@itpassport.local';
