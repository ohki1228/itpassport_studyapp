package com.itpassport.app.auth;

import com.itpassport.app.entity.PasswordResetToken;
import com.itpassport.app.entity.User;
import com.itpassport.app.entity.UserType;
import com.itpassport.app.repository.PasswordResetTokenRepository;
import com.itpassport.app.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * パスワードリセット。セキュリティ上の要点:
 * - トークンはSecureRandomな256bitを使い、推測不可能にする。
 * - 有効期限(30分)・使い捨て(検証後に該当ユーザーの全トークンを削除)。
 * - メールアドレスが登録済みか否かで応答を変えない(列挙攻撃対策。呼び出し元は常に同じ成功表示をする)。
 * - 直近発行から一定時間内は再送しない(同じメールアドレスへの連投=迷惑メール爆弾対策)。
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(30);
    private static final Duration MIN_REQUEST_INTERVAL = Duration.ofMinutes(2);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email)
                .filter(u -> u.getUserType() == UserType.REGISTERED)
                .ifPresent(this::issueTokenAndSend);
    }

    private void issueTokenAndSend(User user) {
        List<PasswordResetToken> recent = tokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (!recent.isEmpty() && Duration.between(recent.get(0).getCreatedAt(), LocalDateTime.now())
                .compareTo(MIN_REQUEST_INTERVAL) < 0) {
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(generateToken());
        resetToken.setExpiresAt(LocalDateTime.now().plus(TOKEN_VALIDITY));
        resetToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
    }

    public boolean isValidToken(String token) {
        return findValid(token).isPresent();
    }

    public String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "パスワードは" + MIN_PASSWORD_LENGTH + "文字以上で入力してください。";
        }
        return null;
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> found = findValid(token);
        if (found.isEmpty()) {
            return false;
        }
        User user = found.get().getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
        return true;
    }

    private Optional<PasswordResetToken> findValid(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
