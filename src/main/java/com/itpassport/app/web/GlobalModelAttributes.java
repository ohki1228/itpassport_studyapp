package com.itpassport.app.web;

import com.itpassport.app.auth.CurrentUserService;
import com.itpassport.app.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 全画面のナビゲーションでログイン状態を表示するため、currentUserを全モデルに注入する。 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final CurrentUserService currentUserService;

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUserService.getCurrentUser();
    }
}
