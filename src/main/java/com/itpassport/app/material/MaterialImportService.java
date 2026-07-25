package com.itpassport.app.material;

import com.itpassport.app.entity.AnswerType;
import com.itpassport.app.entity.Category;
import com.itpassport.app.entity.Genre;
import com.itpassport.app.entity.Knowledge;
import com.itpassport.app.entity.Question;
import com.itpassport.app.entity.QuestionChoice;
import com.itpassport.app.entity.QuestionStatus;
import com.itpassport.app.entity.Term;
import com.itpassport.app.material.dto.GenreRow;
import com.itpassport.app.material.dto.KnowledgeRow;
import com.itpassport.app.material.dto.ParsedMaterials;
import com.itpassport.app.material.dto.QuestionRow;
import com.itpassport.app.material.dto.TermRow;
import com.itpassport.app.repository.CategoryRepository;
import com.itpassport.app.repository.GenreRepository;
import com.itpassport.app.repository.KnowledgeRepository;
import com.itpassport.app.repository.QuestionRepository;
import com.itpassport.app.repository.TermRepository;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Excel(genres/knowledge/questions/terms)の内容をDBへUPSERTする。行単位のバリデーションエラーは
 * その行だけスキップしてErrorsに積み、シート構造自体の異常(シート欠如等)はMaterialParseExceptionとして中断する。
 */
@Service
@RequiredArgsConstructor
public class MaterialImportService {

    private final ExcelMaterialReader reader;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final QuestionRepository questionRepository;
    private final TermRepository termRepository;

    @Transactional
    public ImportResult importFrom(InputStream inputStream) {
        ParsedMaterials parsed = reader.read(inputStream);
        List<String> errors = new ArrayList<>();

        Map<Long, Category> categories = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categories.put(c.getId(), c));

        Map<Long, Long> genreIdToCategoryId = new HashMap<>();
        genreRepository.findAll().forEach(g -> genreIdToCategoryId.put(g.getId(), g.getCategory().getId()));

        int genresImported = importGenres(parsed.genres(), categories, genreIdToCategoryId, errors);
        Set<Long> validGenreIds = genreIdToCategoryId.keySet();
        int knowledgeImported = importKnowledge(parsed.knowledge(), validGenreIds, errors);
        int questionsImported = importQuestions(parsed.questions(), genreIdToCategoryId, errors);
        Set<Long> validKnowledgeIds = new HashSet<>();
        knowledgeRepository.findAll().forEach(k -> validKnowledgeIds.add(k.getId()));
        int termsImported = importTerms(parsed.terms(), validGenreIds, validKnowledgeIds, errors);

        return new ImportResult(genresImported, knowledgeImported, questionsImported, termsImported, errors);
    }

    private int importGenres(List<GenreRow> rows, Map<Long, Category> categories,
                              Map<Long, Long> genreIdToCategoryId, List<String> errors) {
        int count = 0;
        for (GenreRow row : rows) {
            Category category = categories.get(row.categoryId());
            if (category == null) {
                errors.add("genres: genre_id=" + row.genreId() + " category_id=" + row.categoryId() + " が存在しません。");
                continue;
            }
            Genre genre = genreRepository.findById(row.genreId()).orElseGet(Genre::new);
            genre.setId(row.genreId());
            genre.setCategory(category);
            genre.setName(row.genreName());
            genre.setDisplayOrder(row.displayOrder());
            genreRepository.save(genre);
            genreIdToCategoryId.put(row.genreId(), row.categoryId());
            count++;
        }
        return count;
    }

    private int importKnowledge(List<KnowledgeRow> rows, Set<Long> validGenreIds, List<String> errors) {
        int count = 0;
        for (KnowledgeRow row : rows) {
            if (!validGenreIds.contains(row.genreId())) {
                errors.add("knowledge: knowledge_id=" + row.knowledgeId() + " genre_id=" + row.genreId() + " が存在しません。");
                continue;
            }
            Knowledge knowledge = knowledgeRepository.findById(row.knowledgeId()).orElseGet(Knowledge::new);
            knowledge.setId(row.knowledgeId());
            knowledge.setGenre(genreRepository.getReferenceById(row.genreId()));
            knowledge.setTitle(row.title());
            knowledge.setBody(row.body());
            knowledge.setKeywords(row.keywords());
            knowledge.setPoint(row.point());
            knowledge.setUpdatedAt(LocalDateTime.now());
            knowledgeRepository.save(knowledge);
            count++;
        }
        return count;
    }

    private int importTerms(List<TermRow> rows, Set<Long> validGenreIds, Set<Long> validKnowledgeIds,
                             List<String> errors) {
        int count = 0;
        for (TermRow row : rows) {
            if (!validGenreIds.contains(row.genreId())) {
                errors.add("terms: term_id=" + row.termId() + " genre_id=" + row.genreId() + " が存在しません。");
                continue;
            }
            if (!validKnowledgeIds.contains(row.knowledgeId())) {
                errors.add("terms: term_id=" + row.termId() + " knowledge_id=" + row.knowledgeId() + " が存在しません。");
                continue;
            }
            Term term = termRepository.findById(row.termId()).orElseGet(Term::new);
            term.setId(row.termId());
            term.setGenre(genreRepository.getReferenceById(row.genreId()));
            term.setKnowledge(knowledgeRepository.getReferenceById(row.knowledgeId()));
            term.setTerm(row.term());
            term.setReading(row.reading());
            term.setDefinition(row.definition());
            term.setBreakdown(row.breakdown());
            term.setRelatedTerms(row.relatedTerms());
            term.setUpdatedAt(LocalDateTime.now());
            termRepository.save(term);
            count++;
        }
        return count;
    }

    private int importQuestions(List<QuestionRow> rows, Map<Long, Long> genreIdToCategoryId, List<String> errors) {
        int count = 0;
        for (QuestionRow row : rows) {
            List<String> rowErrors = validateQuestionRow(row, genreIdToCategoryId);
            if (!rowErrors.isEmpty()) {
                rowErrors.forEach(msg -> errors.add("questions: question_id=" + row.questionId() + " " + msg));
                continue;
            }

            Question question = questionRepository.findById(row.questionId()).orElseGet(Question::new);
            question.setId(row.questionId());
            question.setCategory(categoryRepository.getReferenceById(row.categoryId()));
            question.setGenre(genreRepository.getReferenceById(row.genreId()));
            question.setQuestionText(row.questionText());
            question.setAnswerType(AnswerType.valueOf(row.answerType().trim().toUpperCase()));
            question.setRequiredAnswerCount(row.requiredAnswerCount());
            question.setExplanation(row.explanation());
            question.setStatus(QuestionStatus.valueOf(row.status().trim().toUpperCase()));
            question.setSource(row.source());
            question.setTableData(row.tableData());
            question.setUpdatedAt(LocalDateTime.now());

            // clear()してから同じchoice_numberで作り直すと、HibernateがDELETEより先にINSERTを
            // 発行して一意制約(question_id, choice_number)に違反することがあるため、
            // 既存の選択肢はchoice_number単位で更新し、無い場合のみ新規追加する。
            Map<Integer, QuestionChoice> existingChoices = new HashMap<>();
            question.getChoices().forEach(c -> existingChoices.put(c.getChoiceNumber(), c));

            Set<Integer> correctSet = new HashSet<>(row.correctAnswers());
            for (int i = 0; i < 4; i++) {
                int choiceNumber = i + 1;
                QuestionChoice choice = existingChoices.get(choiceNumber);
                if (choice == null) {
                    choice = new QuestionChoice();
                    choice.setQuestion(question);
                    choice.setChoiceNumber(choiceNumber);
                    question.getChoices().add(choice);
                }
                choice.setContent(row.choices().get(i));
                choice.setCorrect(correctSet.contains(choiceNumber));
                choice.setExplanation(row.choiceExplanations().get(i));
            }

            questionRepository.save(question);
            count++;
        }
        return count;
    }

    private List<String> validateQuestionRow(QuestionRow row, Map<Long, Long> genreIdToCategoryId) {
        List<String> errors = new ArrayList<>();

        Long actualCategoryId = genreIdToCategoryId.get(row.genreId());
        if (actualCategoryId == null) {
            errors.add("genre_id=" + row.genreId() + " が存在しません。");
            return errors;
        }
        if (!actualCategoryId.equals(row.categoryId())) {
            errors.add("category_id=" + row.categoryId() + " がgenre_id=" + row.genreId()
                    + " の実際の大分類(" + actualCategoryId + ")と一致しません。");
        }

        String answerTypeUpper = row.answerType() == null ? "" : row.answerType().trim().toUpperCase();
        boolean validAnswerType = answerTypeUpper.equals("SINGLE") || answerTypeUpper.equals("MULTIPLE");
        if (!validAnswerType) {
            errors.add("answer_type='" + row.answerType() + "' が不正です(single/multipleのみ許可)。");
        }

        String statusLower = row.status() == null ? "" : row.status().trim().toUpperCase();
        if (!statusLower.equals("PUBLIC") && !statusLower.equals("PRIVATE")) {
            errors.add("status='" + row.status() + "' が不正です(public/privateのみ許可)。");
        }

        List<Integer> correctAnswers = row.correctAnswers();
        if (correctAnswers.size() != row.requiredAnswerCount()) {
            errors.add("correct_answersの個数(" + correctAnswers.size() + ")がrequired_answer_count("
                    + row.requiredAnswerCount() + ")と一致しません。");
        }
        if (new HashSet<>(correctAnswers).size() != correctAnswers.size()) {
            errors.add("correct_answersに重複があります: " + correctAnswers);
        }
        if (correctAnswers.stream().anyMatch(a -> a < 1 || a > 4)) {
            errors.add("correct_answersは1〜4の範囲で指定してください: " + correctAnswers);
        }
        if (validAnswerType && answerTypeUpper.equals("SINGLE") && row.requiredAnswerCount() != 1) {
            errors.add("answer_type=singleの場合、required_answer_countは1である必要があります。");
        }
        if (row.choices().stream().anyMatch(c -> c == null || c.isBlank())) {
            errors.add("choice_1〜4のいずれかが空です。");
        }

        return errors;
    }
}
