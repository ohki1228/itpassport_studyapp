package com.itpassport.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 10)
    private UserType userType;

    /** REGISTEREDのみ設定。GUESTはnull。 */
    @Column(unique = true, length = 255)
    private String email;

    /** REGISTEREDのみ設定。GUESTはnull。 */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** GUESTのみ設定。Cookieに保存し同一ゲストを識別するためのトークン。 */
    @Column(name = "guest_token", unique = true)
    private UUID guestToken;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static User newGuest() {
        User user = new User();
        user.setUserType(UserType.GUEST);
        user.setGuestToken(UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();
        user.setLastActiveAt(now);
        user.setCreatedAt(now);
        return user;
    }
}
