package com.itpassport.app.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * SMTP未設定・送信失敗時でも例外を外へ投げない。呼び出し元(パスワードリセット申請)は
     * メールアドレスの存在有無によらず同じ応答を返す必要があるため、失敗してもリクエスト自体は
     * 成功として扱う(ローカル開発ではリンクをログに出して代用する)。
     */
    public void sendPasswordResetEmail(String to, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        String body = """
                パスワード再設定のリクエストを受け付けました。
                以下のリンクから30分以内に新しいパスワードを設定してください。

                %s

                このメールに心当たりがない場合は、無視していただいて問題ありません。
                """.formatted(link);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            if (!fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setSubject("[ITパスポート学習] パスワード再設定のご案内");
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("パスワードリセットメールの送信に失敗しました(宛先: {}): {}", to, e.getMessage());
            log.info("[開発用] パスワードリセットリンク: {}", link);
        }
    }
}
