-- 用語集を「一行辞書」から脱するための拡張。
-- breakdown: PDCAやSWOT分析のように構成要素を持つ用語の内訳(該当する用語のみ)。
-- related_terms: 関連する用語名(カンマ区切り、knowledgeのkeywordsと同様に自動リンク化する)。
ALTER TABLE terms ADD COLUMN breakdown TEXT;
ALTER TABLE terms ADD COLUMN related_terms TEXT;
