package com.itpassport.app.web;

import com.itpassport.app.auth.CurrentUserService;
import com.itpassport.app.auth.UserPrincipal;
import com.itpassport.app.entity.User;
import com.itpassport.app.entity.UserType;
import com.itpassport.app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 会員登録。ゲスト利用中のユーザーが登録すると、同じUser行をREGISTEREDへ昇格させ
 * それまでの回答履歴を引き継ぐ(要件: ゲスト→登録ユーザーへの履歴引き継ぎ)。
 */
@Controller
@RequiredArgsConstructor
public class RegisterController {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping("/register")
    public String form() {
        if (currentUserService.getCurrentUser().getUserType() == UserType.REGISTERED) {
            return "redirect:/";
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email, @RequestParam String password,
                            @RequestParam(required = false) String displayName, Model model,
                            HttpServletRequest request, HttpServletResponse response) {
        User current = currentUserService.getCurrentUser();
        if (current.getUserType() == UserType.REGISTERED) {
            return "redirect:/";
        }

        String error = validate(email, password);
        if (error != null) {
            model.addAttribute("error", error);
            return "auth/register";
        }

        current.setUserType(UserType.REGISTERED);
        current.setGuestToken(null);
        current.setEmail(email);
        current.setPasswordHash(passwordEncoder.encode(password));
        if (displayName != null && !displayName.isBlank()) {
            current.setDisplayName(displayName);
        }
        current.setLastActiveAt(LocalDateTime.now());
        userRepository.save(current);

        UserPrincipal principal = new UserPrincipal(current);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        // setContext()だけではセッションへ保存されないため、明示的に保存してログイン状態を維持する
        securityContextRepository.saveContext(context, request, response);

        return "redirect:/";
    }

    private String validate(String email, String password) {
        if (email == null || !email.contains("@")) {
            return "メールアドレスの形式が正しくありません。";
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "パスワードは" + MIN_PASSWORD_LENGTH + "文字以上で入力してください。";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return "このメールアドレスは既に登録されています。";
        }
        return null;
    }
}
