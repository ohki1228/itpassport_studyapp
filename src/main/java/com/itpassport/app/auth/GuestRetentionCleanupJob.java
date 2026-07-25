package com.itpassport.app.auth;

import com.itpassport.app.entity.User;
import com.itpassport.app.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 最終アクティブから30日経過したゲストアカウントを削除する(要件: ゲストのデータ保持期間)。
 * study_sessions/answer_historyはON DELETE CASCADEのため一緒に削除される。REGISTEREDは対象外。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GuestRetentionCleanupJob {

    private static final int RETENTION_DAYS = 30;

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpiredGuests() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<User> expired = userRepository.findExpiredGuests(threshold);
        if (!expired.isEmpty()) {
            userRepository.deleteAll(expired);
            log.info("保持期限切れのゲストアカウントを削除しました: {}件", expired.size());
        }
    }
}
