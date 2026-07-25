package com.itpassport.app.web;

import com.itpassport.app.auth.CurrentUserService;
import com.itpassport.app.entity.User;
import com.itpassport.app.stats.GenreStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CurrentUserService currentUserService;
    private final GenreStatsService genreStatsService;

    @GetMapping("/")
    public String home(Model model) {
        User user = currentUserService.getCurrentUser();
        model.addAttribute("overallAccuracy", genreStatsService.getOverallAccuracy(user.getId()));
        model.addAttribute("weakGenres", genreStatsService.getWeakGenres(user.getId()));
        model.addAttribute("genreStats", genreStatsService.getGenreStats(user.getId()));
        return "home";
    }
}
