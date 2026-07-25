# DB設計

要件定義書の8章(Excel管理)をベースに、アプリとして必要な正規化・追加テーブルを加えた設計。

## ER図

```mermaid
erDiagram
    categories ||--o{ genres : has
    genres ||--o{ knowledge : has
    genres ||--o{ terms : has
    knowledge ||--o{ terms : "origin of"
    genres ||--o{ questions : has
    categories ||--o{ questions : has
    questions ||--o{ question_choices : has
    users ||--o{ study_sessions : starts
    users ||--o{ answer_history : answers
    study_sessions ||--o{ answer_history : contains
    questions ||--o{ answer_history : "answered in"
    genres ||--o{ answer_history : "aggregated by"
    answer_history ||--o{ answer_history_selections : has
```

## テーブル定義

### categories(大分類)

ストラテジ系 / マネジメント系 / テクノロジ系の3件固定。Excel管理外、マイグレーションで投入。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGINT PK | 大分類ID |
| name | VARCHAR(50) | 大分類名 |
| display_order | INTEGER | 表示順 |

### genres(ジャンル)

Excel `genres`シートに対応。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGINT PK | Excelの genre_id をそのまま使用 |
| category_id | BIGINT FK→categories | 大分類ID |
| name | VARCHAR(100) | ジャンル名 |
| display_order | INTEGER | 表示順 |

### knowledge(基礎知識)

Excel `knowledge`シートに対応。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGINT PK | Excelの knowledge_id |
| genre_id | BIGINT FK→genres | ジャンルID |
| title | VARCHAR(200) | 単元名 |
| body | TEXT | 基礎解説 |
| keywords | TEXT | 重要語句 |
| point | TEXT | よく出るポイント |
| updated_at | TIMESTAMP | 更新日時(取込み時刻) |

### terms(用語集)

Excel `terms`シートに対応。基礎知識(`knowledge.keywords`)の重要語句を、用語名で突き合わせて自動リンク化するための用語辞書。`term`列にUNIQUE制約を付け、`knowledge.keywords`内の文字列と完全一致した場合のみ`/glossary/{id}`へのリンクとして表示する(未登録語はプレーンテキストのまま)。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGINT PK | Excelの term_id |
| genre_id | BIGINT FK→genres | ジャンルID(用語集一覧のグルーピング用) |
| knowledge_id | BIGINT FK→knowledge(NULL可) | 由来する基礎知識単元。用語集詳細ページから「元になった単元を読む」リンクとして参照する |
| term | VARCHAR(100) UNIQUE | 用語名 |
| reading | VARCHAR(100) | 読み仮名(カタカナ)。用語集の「50音順」表示のソートキーとして使用 |
| definition | TEXT | 意味(簡潔な説明文) |
| breakdown | TEXT(NULL可) | PDCA・SWOT分析など構成要素を持つ用語だけの内訳(「ラベル - 説明」を改行区切りで格納) |
| related_terms | TEXT(NULL可) | 関連する用語名(`、`区切り)。同一の基礎知識単元に由来する語を中心に自動生成し、termとの文字列完全一致でリンク化する |
| updated_at | TIMESTAMP | 更新日時(取込み時刻) |

### questions(問題)

Excel `questions`シートに対応。**choice_1〜4 / correct_answers / explanation_1〜4 は `question_choices` に正規化して持たない。**

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGINT PK | Excelの question_id |
| category_id | BIGINT FK→categories | 大分類ID(genreからも辿れるが、実践演習の出題比率計算で頻繁に使うため非正規化して保持) |
| genre_id | BIGINT FK→genres | ジャンルID |
| question_text | TEXT | 問題文 |
| answer_type | VARCHAR(10) | SINGLE / MULTIPLE |
| required_answer_count | INTEGER | 必要回答数 |
| explanation | TEXT | 全体解説 |
| status | VARCHAR(10) | PUBLIC / PRIVATE |
| source | VARCHAR(255)(NULL可) | 過去問等、外部から引用した問題の出典表記。自作問題はNULL |
| table_data | TEXT(NULL可) | 問題文に添付する表(行=改行区切り、セル=`\|`区切り、1行目は見出し行)。`Question.getTableRows()`で行×セルのリストに分解し、テンプレート側で`<table>`として描画する |
| updated_at | TIMESTAMP | 更新日時(取込み時刻) |

### question_choices(選択肢)

1問につき4件。`is_correct` が正解フラグ。Excelの correct_answers(カンマ区切り)は取込み時にここへ展開する。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGSERIAL PK | |
| question_id | BIGINT FK→questions | |
| choice_number | INTEGER(1〜4) | 選択肢番号(JPAのInteger型に合わせるためSMALLINTではなくINTEGERを使用) |
| content | TEXT | 選択肢文 |
| is_correct | BOOLEAN | 正解かどうか |
| explanation | TEXT | この選択肢の「なぜ正しいか/なぜ間違いか」 |

UNIQUE(question_id, choice_number)

### users(学習者)

ログイン不要でも使えるよう、ゲスト利用(UUIDトークン)と会員登録(email/password)の両方に対応する。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGSERIAL PK | |
| user_type | VARCHAR(10) | GUEST / REGISTERED |
| email | VARCHAR(255) UNIQUE(NULL可) | REGISTERED専用。ログインID |
| password_hash | VARCHAR(255)(NULL可) | REGISTERED専用。ハッシュ化済みパスワード |
| guest_token | UUID UNIQUE(NULL可) | GUEST専用。ブラウザのCookieに保存し同一ゲストを識別する |
| display_name | VARCHAR(100) | 表示名(任意) |
| last_active_at | TIMESTAMP | 最終アクティブ日時。ゲストの保持期限判定に使用 |
| created_at | TIMESTAMP | 登録日時 |

CHECK制約で `user_type=REGISTERED なら email/password_hash必須・guest_token NULL`、`user_type=GUEST なら guest_token必須・email/password_hash NULL` を強制する。

**ゲストのデータ保持方針**: `last_active_at` から30日間操作のないGUESTユーザーは、定期ジョブ(`@Scheduled`)で`study_sessions`/`answer_history`ごとCASCADE削除する(REGISTEREDユーザーは対象外、無期限保持)。そのため`study_sessions.user_id`・`answer_history.user_id`の外部キーは`ON DELETE CASCADE`にしている。

**ゲスト→会員登録の引き継ぎ**: ゲストが会員登録すると、同じ`users`行を`user_type=REGISTERED`に書き換え、`guest_token`をNULLにしてemail/password_hashを設定する(idはそのまま)。これにより回答履歴を保持したままアカウント化できる。

### study_sessions(演習セッション)

演習/苦手克服/実践演習の「1回分のまとまり」。実践演習の制限時間・終了状態や、結果画面の集計単位として使う。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK→users | |
| mode | VARCHAR(20) | PRACTICE / WEAKNESS / MOCK_EXAM |
| question_count | INTEGER(NULL可) | 出題数。エンドレスはNULL |
| time_limit_minutes | INTEGER(NULL可) | 実践演習のみ120、他はNULL |
| started_at | TIMESTAMP | 開始日時 |
| finished_at | TIMESTAMP(NULL可) | 終了日時 |

### answer_history(回答履歴)

要件10章に対応。1問1回答につき1レコード。

| 列名 | 型 | 内容 |
|---|---|---|
| id | BIGSERIAL PK | |
| session_id | BIGINT FK→study_sessions | |
| user_id | BIGINT FK→users | 非正規化(集計クエリの簡略化用) |
| question_id | BIGINT FK→questions | |
| genre_id | BIGINT FK→genres | 非正規化(苦手ジャンル集計用) |
| is_correct | BOOLEAN | 正解/不正解 |
| mode | VARCHAR(20) | PRACTICE / WEAKNESS / MOCK_EXAM |
| answered_at | TIMESTAMP | 回答日時 |

### answer_history_selections(選択した回答)

複数回答対応のため、選択した選択肢番号を行持ちで保持(CSV文字列では持たない)。

| 列名 | 型 | 内容 |
|---|---|---|
| answer_history_id | BIGINT FK→answer_history | |
| choice_number | INTEGER | 選択した選択肢番号 |

PRIMARY KEY(answer_history_id, choice_number)

## インデックス

- `answer_history(user_id, genre_id)` — ジャンル別正答率・苦手判定の集計用
- `answer_history(user_id, mode, answered_at)` — モード別履歴表示用
- `questions(genre_id, status)` — 出題対象の絞り込み用
- `knowledge(genre_id)` — ジャンル別基礎知識表示用

## 設計上の主な決定事項(要件書からの変更点)

1. **選択肢の正規化**: Excelの `choice_1〜4` / `explanation_1〜4` / `correct_answers` は `question_choices` テーブルへ展開。取込みバッチが1問→4行に変換する。
2. **複数回答の正規化**: `selected_answers` はCSVではなく `answer_history_selections` で行持ち管理。
3. **categoriesを独立テーブル化**: Excelに大分類シートは無いが、実践演習の出題比率計算(35:20:45)で頻繁に使うため固定マスタとして用意。
4. **study_sessionsを新設**: 要件に無いが、「25/50/100問演習」「実践演習120分」を1つのまとまりとして管理し、結果画面・時間制限の実装に使う。
5. **PKはExcelのIDをそのまま使用**: genre_id / knowledge_id / question_id はExcel側で採番済みのため、DBのPKとしてそのまま利用。再取込み時はUPSERT(ID一致で更新)する想定。

## Railway PostgreSQL運用に関する補足

DBはRailway上のPostgreSQL(素のPostgreSQL、拡張なし)を利用する。テーブル定義自体に変更は不要だが、プロジェクト雛形作成(次々工程)で以下を踏まえる。

- **マイグレーション管理**: `db/schema.sql` はあくまで設計リファレンス。実装時はFlywayを導入し、`src/main/resources/db/migration/V1__init.sql` として同内容を配置してSpring Boot起動時に自動適用する。手動でのCREATE TABLE運用はしない。
- **接続情報**: RailwayはPostgres接続情報を `DATABASE_URL`(postgres://user:pass@host:port/db 形式)または `PGHOST`/`PGPORT`/`PGUSER`/`PGPASSWORD`/`PGDATABASE` の個別環境変数で提供する。Spring Bootの `spring.datasource.url` はJDBC形式(`jdbc:postgresql://...`)を要求するため、`application.properties` で環境変数から組み立てるか、変換用の設定を用意する。
- **SSL**: Railway外部からの接続時はSSL必須になる場合がある。同一Railwayプロジェクト内(アプリ→DB)の内部接続ではSSL不要なことが多いが、`sslmode=prefer` 程度は設定しておくと安全。
- **ローカル開発との差分**: ローカルはDockerや直接インストールしたPostgreSQLを使い、`application-local.properties` 等でプロファイル分離する想定(接続先以外の差分は無し)。
