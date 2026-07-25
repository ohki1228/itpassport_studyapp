-- 過去問等、外部から引用した問題の出典表記。自作問題はNULLのまま。
ALTER TABLE questions ADD COLUMN source VARCHAR(255);
