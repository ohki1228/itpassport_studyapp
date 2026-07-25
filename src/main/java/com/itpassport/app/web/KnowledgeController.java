package com.itpassport.app.web;

import com.itpassport.app.entity.Genre;
import com.itpassport.app.entity.Knowledge;
import com.itpassport.app.entity.Term;
import com.itpassport.app.repository.CategoryRepository;
import com.itpassport.app.repository.GenreRepository;
import com.itpassport.app.repository.KnowledgeRepository;
import com.itpassport.app.repository.TermRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequiredArgsConstructor
public class KnowledgeController {

    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final TermRepository termRepository;

    @GetMapping("/knowledge")
    public String list(Model model) {
        var categoryGenres = categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> new CategoryGenres(
                        category, genreRepository.findAllByCategoryIdOrderByDisplayOrderAsc(category.getId())))
                .toList();
        model.addAttribute("categoryGenres", categoryGenres);
        return "knowledge/list";
    }

    @GetMapping("/knowledge/{genreId}")
    public String genreUnits(@PathVariable Long genreId, Model model) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ジャンルが見つかりません。"));
        model.addAttribute("genre", genre);
        model.addAttribute("knowledgeList", knowledgeRepository.findAllByGenreIdOrderByIdAsc(genreId));
        return "knowledge/genre";
    }

    @GetMapping("/knowledge/{genreId}/{knowledgeId}")
    public String unitDetail(@PathVariable Long genreId, @PathVariable Long knowledgeId, Model model) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ジャンルが見つかりません。"));
        List<Knowledge> knowledgeList = knowledgeRepository.findAllByGenreIdOrderByIdAsc(genreId);

        int index = -1;
        for (int i = 0; i < knowledgeList.size(); i++) {
            if (knowledgeList.get(i).getId().equals(knowledgeId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "単元が見つかりません。");
        }
        Knowledge current = knowledgeList.get(index);
        Knowledge prev = index > 0 ? knowledgeList.get(index - 1) : null;
        Knowledge next = index < knowledgeList.size() - 1 ? knowledgeList.get(index + 1) : null;

        Map<String, Long> termIndex = termRepository.findAll().stream()
                .collect(Collectors.toMap(Term::getTerm, Term::getId, (a, b) -> a));

        model.addAttribute("genre", genre);
        model.addAttribute("knowledge", current);
        model.addAttribute("keywordLinks", parseKeywords(current.getKeywords(), termIndex));
        model.addAttribute("prev", prev);
        model.addAttribute("next", next);
        model.addAttribute("unitNumber", index + 1);
        model.addAttribute("unitCount", knowledgeList.size());
        return "knowledge/detail";
    }

    private List<KeywordLink> parseKeywords(String keywords, Map<String, Long> termIndex) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split("、"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> new KeywordLink(s, termIndex.get(s)))
                .toList();
    }
}
