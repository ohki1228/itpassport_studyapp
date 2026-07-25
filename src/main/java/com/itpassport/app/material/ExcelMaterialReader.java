package com.itpassport.app.material;

import com.itpassport.app.material.dto.GenreRow;
import com.itpassport.app.material.dto.KnowledgeRow;
import com.itpassport.app.material.dto.ParsedMaterials;
import com.itpassport.app.material.dto.QuestionRow;
import com.itpassport.app.material.dto.TermRow;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * genres / knowledge / questions / terms シートを読み、型変換のみを行う(業務的な整合性チェックはMaterialImportServiceで行う)。
 */
@Component
public class ExcelMaterialReader {

    private static final DataFormatter FORMATTER = new DataFormatter();

    public ParsedMaterials read(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            List<GenreRow> genres = readGenres(requireSheet(workbook, "genres"));
            List<KnowledgeRow> knowledge = readKnowledge(requireSheet(workbook, "knowledge"));
            List<QuestionRow> questions = readQuestions(requireSheet(workbook, "questions"));
            List<TermRow> terms = readTerms(requireSheet(workbook, "terms"));
            return new ParsedMaterials(genres, knowledge, questions, terms);
        } catch (IOException e) {
            throw new MaterialParseException("Excelファイルの読み込みに失敗しました: " + e.getMessage());
        }
    }

    private Sheet requireSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new MaterialParseException("シート '" + name + "' が見つかりません。");
        }
        return sheet;
    }

    private List<GenreRow> readGenres(Sheet sheet) {
        List<GenreRow> rows = new ArrayList<>();
        for (Row row : dataRows(sheet)) {
            rows.add(new GenreRow(
                    requiredLong(row, 0, "genres", "genre_id"),
                    requiredLong(row, 1, "genres", "category_id"),
                    requiredString(row, 2, "genres", "category_name"),
                    requiredString(row, 3, "genres", "genre_name"),
                    (int) requiredLong(row, 4, "genres", "display_order")
            ));
        }
        return rows;
    }

    private List<KnowledgeRow> readKnowledge(Sheet sheet) {
        List<KnowledgeRow> rows = new ArrayList<>();
        for (Row row : dataRows(sheet)) {
            rows.add(new KnowledgeRow(
                    requiredLong(row, 0, "knowledge", "knowledge_id"),
                    requiredLong(row, 1, "knowledge", "genre_id"),
                    requiredString(row, 2, "knowledge", "title"),
                    requiredString(row, 3, "knowledge", "body"),
                    optionalString(row, 4),
                    optionalString(row, 5)
            ));
        }
        return rows;
    }

    private List<QuestionRow> readQuestions(Sheet sheet) {
        List<QuestionRow> rows = new ArrayList<>();
        for (Row row : dataRows(sheet)) {
            long questionId = requiredLong(row, 0, "questions", "question_id");
            List<String> choices = List.of(
                    requiredString(row, 5, "questions", "choice_1"),
                    requiredString(row, 6, "questions", "choice_2"),
                    requiredString(row, 7, "questions", "choice_3"),
                    requiredString(row, 8, "questions", "choice_4")
            );
            List<Integer> correctAnswers = parseCorrectAnswers(row, questionId);
            List<String> choiceExplanations = List.of(
                    requiredString(row, 13, "questions", "explanation_1"),
                    requiredString(row, 14, "questions", "explanation_2"),
                    requiredString(row, 15, "questions", "explanation_3"),
                    requiredString(row, 16, "questions", "explanation_4")
            );
            rows.add(new QuestionRow(
                    questionId,
                    requiredLong(row, 1, "questions", "category_id"),
                    requiredLong(row, 2, "questions", "genre_id"),
                    requiredString(row, 3, "questions", "question"),
                    optionalString(row, 4),
                    choices,
                    requiredString(row, 9, "questions", "answer_type"),
                    correctAnswers,
                    (int) requiredLong(row, 11, "questions", "required_answer_count"),
                    requiredString(row, 12, "questions", "explanation"),
                    choiceExplanations,
                    requiredString(row, 17, "questions", "status"),
                    optionalString(row, 18)
            ));
        }
        return rows;
    }

    private List<TermRow> readTerms(Sheet sheet) {
        List<TermRow> rows = new ArrayList<>();
        for (Row row : dataRows(sheet)) {
            rows.add(new TermRow(
                    requiredLong(row, 0, "terms", "term_id"),
                    requiredLong(row, 1, "terms", "genre_id"),
                    requiredLong(row, 2, "terms", "knowledge_id"),
                    requiredString(row, 3, "terms", "term"),
                    requiredString(row, 4, "terms", "reading"),
                    requiredString(row, 5, "terms", "definition"),
                    optionalString(row, 6),
                    optionalString(row, 7)
            ));
        }
        return rows;
    }

    private List<Integer> parseCorrectAnswers(Row row, long questionId) {
        String raw = requiredString(row, 10, "questions", "correct_answers");
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new MaterialParseException(
                    "questions行(question_id=" + questionId + ")のcorrect_answersが不正です: " + raw);
        }
    }

    private Iterable<Row> dataRows(Sheet sheet) {
        List<Row> rows = new ArrayList<>();
        int last = sheet.getLastRowNum();
        for (int i = 1; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlankRow(row)) {
                continue;
            }
            rows.add(row);
        }
        return rows;
    }

    private boolean isBlankRow(Row row) {
        Cell first = row.getCell(0);
        return first == null || FORMATTER.formatCellValue(first).isBlank();
    }

    private String requiredString(Row row, int col, String sheetName, String columnName) {
        String value = optionalString(row, col);
        if (value == null || value.isBlank()) {
            throw new MaterialParseException(
                    "シート'" + sheetName + "' " + (row.getRowNum() + 1) + "行目: '" + columnName + "' が空です。");
        }
        return value;
    }

    private String optionalString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    private long requiredLong(Row row, int col, String sheetName, String columnName) {
        String value = requiredString(row, col, sheetName, columnName);
        try {
            return (long) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new MaterialParseException(
                    "シート'" + sheetName + "' " + (row.getRowNum() + 1) + "行目: '" + columnName
                            + "' は数値である必要があります(値: " + value + ")");
        }
    }
}
