package com.itpassport.app.web;

import com.itpassport.app.auth.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String form() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submit(@RequestParam String email, Model model) {
        // メールアドレスが登録されているかどうかによらず同じ応答にする(列挙攻撃対策)。
        passwordResetService.requestReset(email);
        model.addAttribute("submitted", true);
        return "auth/forgot-password";
    }
}
