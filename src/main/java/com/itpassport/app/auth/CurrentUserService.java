package com.itpassport.app.auth;

import com.itpassport.app.entity.User;
import com.itpassport.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * ログイン中ユーザー(またはGuestAuthenticationFilterが割り当てたゲスト)を取得する窓口。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("認証情報が見つかりません。GuestAuthenticationFilterの設定を確認してください。");
        }
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません: id=" + principal.getUserId()));
    }
}
