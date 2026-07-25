# Excel教材フォーマット仕様

要件定義書8章をベースに、取込みバッチ実装に必要なレベルまでルールを明確化したもの。ファイルは1冊(.xlsx)に `genres` / `knowledge` / `questions` の3シートを持つ構成。サンプル: `db/sample-data/study-materials-sample.xlsx`

## 共通ルール

- 1行目はヘッダー行(列名)。2行目以降がデータ。
- 各シートのID列(genre_id / knowledge_id / question_id)は**シート内で一意**、かつ**空行不可**。
- 取込みバッチはID列をキーにUPSERTする(同じIDで再取込みすると内容が更新される)。
- 文字コード/改行はExcelのセル内改行(Alt+Enter)をそのまま許可し、`body`や`question`等の複数行テキストに使える。

## genresシート

| 列名 | 型/制約 | 備考 |
|---|---|---|
| genre_id | 整数, 一意 | 例: 101 |
| category_id | 整数, 1=ストラテジ系 / 2=マネジメント系 / 3=テクノロジ系 | DBの`categories`固定マスタに対応 |
| category_name | 文字列 | 表示用。category_idとの整合性は取込み時にチェックし、不一致ならエラー |
| genre_name | 文字列 | 例: 企業と法務 |
| display_order | 整数 | ホーム画面・演習画面での並び順 |

要件5章の9ジャンルをそのまま初期データとする(下表)。

| category_id | category_name | genre_id | genre_name |
|---|---|---|---|
| 1 | ストラテジ系 | 101 | 企業と法務 |
| 1 | ストラテジ系 | 102 | 経営戦略 |
| 1 | ストラテジ系 | 103 | システム戦略 |
| 2 | マネジメント系 | 201 | 開発技術 |
| 2 | マネジメント系 | 202 | プロジェクトマネジメント |
| 2 | マネジメント系 | 203 | サービスマネジメント |
| 3 | テクノロジ系 | 301 | 基礎理論 |
| 3 | テクノロジ系 | 302 | コンピュータシステム |
| 3 | テクノロジ系 | 303 | 技術要素 |

genre_idは「大分類ID(1桁) + 連番(2桁)」の採番ルールとする(例: 101 = category_id 1 の1番目)。

## knowledgeシート

| 列名 | 型/制約 | 備考 |
|---|---|---|
| knowledge_id | 整数, 一意 | 例: 10101(genre_id + 連番2桁) |
| genre_id | 整数, genresシートに存在するID | |
| title | 文字列 | 単元名 |
| body | 文字列(長文可) | 基礎解説 |
| keywords | 文字列, `、`区切り | 重要語句を複数列挙。`termsシート`の`term`列と完全一致する語は画面上で用語集へのリンクとして表示される |
| point | 文字列(長文可) | よく出るポイント |

## termsシート

基礎知識の重要語句(keywords)を用語集としてリンク化するための用語辞書。

| 列名 | 型/制約 | 備考 |
|---|---|---|
| term_id | 整数, 一意 | 例: 10101(genre_id + 連番2桁) |
| genre_id | 整数, genresシートに存在するID | 用語集一覧のグルーピング用 |
| knowledge_id | 整数, knowledgeシートに存在するID | 由来する基礎知識単元。用語集詳細ページに「この単元を読む」リンクとして表示する |
| term | 文字列, シート内で一意 | 用語名。knowledgeシートのkeywordsと文字列完全一致した場合のみリンク化される |
| reading | 文字列, カタカナ | 50音順ソート用の読み仮名 |
| definition | 文字列(長文可) | 意味(簡潔な説明文) |
| breakdown | 文字列(改行区切り、任意) | PDCA・SWOT分析のように構成要素を持つ用語だけの内訳。「ラベル - 説明」を1行ずつ記載。無い用語は空欄でよい |
| related_terms | 文字列, `、`区切り(任意) | 関連する用語名。termシート内の別の用語名と文字列完全一致した場合のみリンク化される |

## questionsシート

| 列名 | 型/制約 | 備考 |
|---|---|---|
| question_id | 整数, 一意 | 例: 101001(genre_id×1000 + 連番3桁) |
| category_id | 整数 | genre_idの大分類と一致必須 |
| genre_id | 整数, genresシートに存在するID | |
| question | 文字列(長文可) | 問題文 |
| table_data | 文字列(任意) | 問題文に添付する表。行は改行区切り、セルは`\|`区切り、1行目は見出し行として扱う。表が不要な問題は空欄。例: `(単位:億円)\|市況が好転\|市況が悪化`→改行→`投資戦略a\|20\|-15` |
| choice_1〜choice_4 | 文字列 | 選択肢1〜4。4択固定(空欄不可) |
| answer_type | `single` / `multiple` | |
| correct_answers | 整数をカンマ区切り(例: `4` または `1,3`) | choice_1〜4の番号(1〜4)を指定。single時は1個、multiple時は複数指定 |
| required_answer_count | 整数 | correct_answersの個数と一致必須(取込み時にチェック) |
| explanation | 文字列(長文可) | 全体解説 |
| explanation_1〜explanation_4 | 文字列 | 各選択肢が「なぜ正しいか/なぜ間違いか」 |
| status | `public` / `private` | privateは出題対象外(下書き用) |
| source | 文字列(任意) | 過去問等、外部から引用した問題の出典。自作問題は空欄。例: 「出典：令和8年度 ITパスポート試験 公開問題 問1（独立行政法人情報処理推進機構）」 |

### 取込み時のバリデーション(エラーとして扱う)

- genre_id / category_id が genresシートに存在しない
- correct_answersの個数 ≠ required_answer_count
- correct_answersに1〜4以外の値、または重複値が含まれる
- answer_type=singleなのにrequired_answer_count≠1
- choice_1〜4のいずれかが空
- ID重複(同一シート内でgenre_id/knowledge_id/question_idが重複、ただしUPSERT対象としてWARNログのみで処理継続でも可・要検討)

## サンプルデータの範囲

`study-materials-sample.xlsx` には動作確認用として以下を収録する(本番用の全問題データは別途作成)。

- genres: 9件(全ジャンル)
- knowledge: IPA公式シラバスVer.6.5の63項目と1対1で対応させ、各ジャンル1〜14件、計63件
- questions: IPA公式の過去問(令和3〜8年度 ITパスポート試験 公開問題、各年度問1〜問100)を全問、出典明記の上で収録、計600件(自作問題は廃止し、過去問のみに統一)
- terms: knowledgeの重要語句(keywords)を重複除去した440件に加え、過去問600問の解説から未収録用語220件を追加、計660件
