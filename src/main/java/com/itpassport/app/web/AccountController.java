package com.itpassport.app.web;

import com.itpassport.app.auth.CurrentUserService;
import com.itpassport.app.auth.GuestAuthenticationFilter;
import com.itpassport.app.entity.User;
import com.itpassport.app.entity.UserType;
import com.itpassport.app.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * アカウント削除。REGISTEREDはパスワード確認必須、GUESTは確認ボタンのみで即時削除する
 * (要件: プライバシーポリシーに記載したデータ削除の実体)。study_sessions/answer_historyは
 * users.idへのON DELETE CASCADEで一緒に削除される。
 */
@Controller
@RequiredArgsConstructor
public class AccountController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/account/delete")
    public String confirmForm(Model model) {
        model.addAttribute("targetUserType", currentUserService.getCurrentUser().getUserType());
        return "account/delete";
    }

    @PostMapping("/account/delete")
    public String delete(@RequestParam(required = false) String password,
                          HttpServletRequest request, HttpServletResponse response, Model model) {
        User user = currentUserService.getCurrentUser();

        if (user.getUserType() == UserType.REGISTERED) {
            if (password == null || user.getPasswordHash() == null
                    || !passwordEncoder.matches(password, user.getPasswordHash())) {
                model.addAttribute("targetUserType", user.getUserType());
                model.addAttribute("error", "パスワードが正しくありません。");
                return "account/delete";
            }
        } else {
            clearGuestCookie(response);
        }

        userRepository.delete(user);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return "redirect:/?accountDeleted";
    }

    private void clearGuestCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(GuestAuthenticationFilter.GUEST_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
