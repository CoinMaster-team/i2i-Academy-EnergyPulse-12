package com.coinmaster.energypulse.demo;

import com.coinmaster.energypulse.auth.domain.UserAccount;
import com.coinmaster.energypulse.auth.repository.UserAccountRepository;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import com.coinmaster.energypulse.home.service.HomeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

    @Mock
    private HomeRepository homeRepository;

    @Mock
    private HomeService homeService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Test
    void shouldCreateEvaluationUserEvenWhenDemoHomesAlreadyExist() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        when(homeRepository.count()).thenReturn(1L);

        DemoDataInitializer initializer = new DemoDataInitializer(
                homeRepository,
                homeService,
                jdbcTemplate,
                userAccountRepository,
                passwordEncoder,
                "demo@energypulse.local",
                "EnergyPulse Admin",
                " ADMIN@EnergyPulse.com ",
                "EnergyPulse2026!");

        initializer.run(null);

        ArgumentCaptor<UserAccount> userCaptor =
                ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());

        UserAccount savedUser = userCaptor.getValue();
        assertEquals("EnergyPulse Admin", savedUser.getFullName());
        assertEquals("admin@energypulse.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches(
                "EnergyPulse2026!",
                savedUser.getPasswordHash()));
    }
}
