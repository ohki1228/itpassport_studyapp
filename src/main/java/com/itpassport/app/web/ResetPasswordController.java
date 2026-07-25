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
public class ResetPasswordController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/reset-password")
    public String form(@RequestParam String token, Model model) {
        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String submit(@RequestParam String token, @RequestParam String password, Model model) {
        String validationError = passwordResetService.validatePassword(password);
        if (validationError != null) {
            model.addAttribute("token", token);
            model.addAttribute("error", validationError);
            return "auth/reset-password";
        }

        boolean success = passwordResetService.resetPassword(token, password);
        if (!success) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }
        return "redirect:/login?reset";
    }
}
