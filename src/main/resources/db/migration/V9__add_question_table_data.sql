-- 問題文に添付する表形式データ。行は改行区切り、セルは「|」区切り。1行目は見出し行として扱う。
-- 例: "(単位:億円)|市況が好転|市況が悪化\n投資戦略a|20|-15\n投資戦略b|5|0"
ALTER TABLE questions ADD COLUMN table_data TEXT;
