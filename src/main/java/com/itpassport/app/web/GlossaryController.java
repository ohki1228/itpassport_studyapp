package com.itpassport.app.web;

import com.itpassport.app.entity.Term;
import com.itpassport.app.repository.CategoryRepository;
import com.itpassport.app.repository.GenreRepository;
import com.itpassport.app.repository.TermRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class GlossaryController {

    private static final String[] GOJUON_IDS = {"a", "ka", "sa", "ta", "na", "ha", "ma", "ya", "ra", "wa"};
    private static final String[] GOJUON_LABELS = {"あ", "か", "さ", "た", "な", "は", "ま", "や", "ら", "わ"};
    private static final String[] GOJUON_CHARS = {
            "アイウエオァィゥェォヴ",
            "カキクケコガギグゲゴ",
            "サシスセソザジズゼゾ",
            "タチツテトダヂヅデドッ",
            "ナニヌネノ",
            "ハヒフヘホバビブベボパピプペポ",
            "マミムメモ",
            "ヤユヨャュョ",
            "ラリルレロ",
            "ワヲンヮ",
    };

    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final TermRepository termRepository;

    @GetMapping("/glossary")
    public String list(@RequestParam(defaultValue = "genre") String view, Model model) {
        model.addAttribute("view", view);

        if ("reading".equals(view)) {
            model.addAttribute("gojuonRows", groupByGojuonRow(termRepository.findAllByOrderByReadingAsc()));
            return "glossary/list";
        }

        var categoryGenres = categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> new CategoryGenres(
                        category, genreRepository.findAllByCategoryIdOrderByDisplayOrderAsc(category.getId())))
                .toList();

        Map<Long, List<Term>> termsByGenreId = termRepository.findAllByOrderByGenreIdAscTermAsc().stream()
                .collect(Collectors.groupingBy(t -> t.getGenre().getId(), LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("categoryGenres", categoryGenres);
        model.addAttribute("termsByGenreId", termsByGenreId);
        return "glossary/list";
    }

    @GetMapping("/glossary/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Term term = termRepository.findByIdWithGenreAndKnowledge(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用語が見つかりません。"));

        Map<String, Long> termIndex = termRepository.findAll().stream()
                .collect(Collectors.toMap(Term::getTerm, Term::getId, (a, b) -> a));

        model.addAttribute("term", term);
        model.addAttribute("relatedTermLinks", parseRelatedTerms(term.getRelatedTerms(), termIndex));
        return "glossary/detail";
    }

    private List<KeywordLink> parseRelatedTerms(String relatedTerms, Map<String, Long> termIndex) {
        if (relatedTerms == null || relatedTerms.isBlank()) {
            return List.of();
        }
        return Arrays.stream(relatedTerms.split("、"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> new KeywordLink(s, termIndex.get(s)))
                .toList();
    }

    private List<GojuonRow> groupByGojuonRow(List<Term> terms) {
        List<List<Term>> buckets = new ArrayList<>();
        for (int i = 0; i < GOJUON_LABELS.length; i++) {
            buckets.add(new ArrayList<>());
        }
        for (Term term : terms) {
            int rowIndex = resolveRowIndex(term.getReading());
            buckets.get(rowIndex).add(term);
        }
        List<GojuonRow> rows = new ArrayList<>();
        for (int i = 0; i < GOJUON_LABELS.length; i++) {
            if (!buckets.get(i).isEmpty()) {
                rows.add(new GojuonRow(GOJUON_IDS[i], GOJUON_LABELS[i], buckets.get(i)));
            }
        }
        return rows;
    }

    private int resolveRowIndex(String reading) {
        if (reading != null && !reading.isEmpty()) {
            char first = reading.charAt(0);
            for (int i = 0; i < GOJUON_CHARS.length; i++) {
                if (GOJUON_CHARS[i].indexOf(first) >= 0) {
                    return i;
                }
            }
        }
        return GOJUON_LABELS.length - 1;
    }
}
