package co.edu.uniminuto.hyeda.iam.service;

import co.edu.uniminuto.hyeda.iam.domain.Role;
import co.edu.uniminuto.hyeda.iam.domain.UserAccount;
import co.edu.uniminuto.hyeda.iam.repository.RoleRepository;
import co.edu.uniminuto.hyeda.iam.repository.UserAccountRepository;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;

/**
 * Crea la cuenta de administradora la primera vez que arranca el sistema.
 * Los roles y permisos ya vienen de las migraciones Flyway.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminAccountSeeder {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HyedaProperties properties;

    @Bean
    public ApplicationRunner seedAdminAccount() {
        return args -> createAdminIfMissing();
    }

    @Transactional
    void createAdminIfMissing() {
        HyedaProperties.Admin admin = properties.getAdmin();
        if (userRepository.existsByUsernameIgnoreCase(admin.getUsername())) {
            return;
        }

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException(
                        "El rol ADMINISTRADOR no existe; revisa la migracion V2__seed_reference_data.sql"));

        UserAccount account = UserAccount.builder()
                .username(admin.getUsername())
                .passwordHash(passwordEncoder.encode(admin.getPassword()))
                .fullName(admin.getFullName())
                .phone(admin.getPhone())
                .active(true)
                .mustChangePassword(false)
                .roles(new LinkedHashSet<>(java.util.List.of(adminRole)))
                .build();

        userRepository.save(account);
        log.info("Cuenta de administracion creada: {}", admin.getUsername());
    }
}
