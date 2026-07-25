package com.itpassport.app.auth;

import com.itpassport.app.entity.User;
import com.itpassport.app.entity.UserType;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final UserType userType;

    public UserPrincipal(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.userType = user.getUserType();
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = userType == UserType.GUEST ? "ROLE_GUEST" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return userType == UserType.GUEST ? "guest:" + userId : email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
