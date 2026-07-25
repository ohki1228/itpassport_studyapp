package com.itpassport.app.auth;

import com.itpassport.app.entity.UserType;
import com.itpassport.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .filter(u -> u.getUserType() == UserType.REGISTERED)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));
    }
}
