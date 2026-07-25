package com.itpassport.app.auth;

import com.itpassport.app.entity.User;
import com.itpassport.app.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ログイン中(REGISTERED)でなければ、Cookieのゲストトークンから既存ゲストを復元するか、
 * 新規ゲストを発行してSecurityContextへセットする。これにより未ログインでも全機能を使える。
 */
@Component
@RequiredArgsConstructor
public class GuestAuthenticationFilter extends OncePerRequestFilter {

    public static final String GUEST_COOKIE_NAME = "guest_token";
    private static final int GUEST_COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60; // 保持期間(30日)に合わせる
    private static final Duration LAST_ACTIVE_UPDATE_INTERVAL = Duration.ofHours(1);

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        boolean alreadyAuthenticated = existing != null && existing.isAuthenticated()
                && !(existing instanceof AnonymousAuthenticationToken);

        if (!alreadyAuthenticated) {
            User guest = resolveOrCreateGuest(request, response);
            UserPrincipal principal = new UserPrincipal(guest);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }

    @Transactional
    protected User resolveOrCreateGuest(HttpServletRequest request, HttpServletResponse response) {
        Optional<UUID> token = readGuestCookie(request);
        if (token.isPresent()) {
            Optional<User> existing = userRepository.findByGuestToken(token.get());
            if (existing.isPresent()) {
                touchLastActiveIfStale(existing.get());
                return existing.get();
            }
        }

        User guest = User.newGuest();
        userRepository.save(guest);
        writeGuestCookie(request, response, guest.getGuestToken());
        return guest;
    }

    private void touchLastActiveIfStale(User guest) {
        if (Duration.between(guest.getLastActiveAt(), LocalDateTime.now()).compareTo(LAST_ACTIVE_UPDATE_INTERVAL) >= 0) {
            guest.setLastActiveAt(LocalDateTime.now());
            userRepository.save(guest);
        }
    }

    private Optional<UUID> readGuestCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (GUEST_COOKIE_NAME.equals(cookie.getName())) {
                try {
                    return Optional.of(UUID.fromString(cookie.getValue()));
                } catch (IllegalArgumentException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private void writeGuestCookie(HttpServletRequest request, HttpServletResponse response, UUID token) {
        Cookie cookie = new Cookie(GUEST_COOKIE_NAME, token.toString());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(GUEST_COOKIE_MAX_AGE_SECONDS);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
