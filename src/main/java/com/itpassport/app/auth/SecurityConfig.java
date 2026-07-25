package com.itpassport.app.auth;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 未ログインでも全画面をゲスト利用できるようにし(GuestAuthenticationFilterが自動でゲストを割り当てる)、
 * /login からのフォーム認証で登録ユーザーとしてログインできるようにする。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GuestAuthenticationFilter guestAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 会員登録直後にSecurityContextへ認証情報をセットしてもセッションへは自動保存されないため、
     * RegisterControllerから明示的にsaveContext()するために公開する。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * GuestAuthenticationFilterはSpring Securityのfilter chain内でのみ使う。
     * @Componentのままだと、Spring BootがServletコンテナへも自動登録してしまい
     * (GenericFilterBeanの初期化を経ないため)NullPointerExceptionで起動に失敗するため無効化する。
     */
    @Bean
    public FilterRegistrationBean<GuestAuthenticationFilter> guestAuthenticationFilterRegistration(
            GuestAuthenticationFilter filter) {
        FilterRegistrationBean<GuestAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .addFilterAfter(guestAuthenticationFilter, SecurityContextHolderFilter.class);
        return http.build();
    }
}
