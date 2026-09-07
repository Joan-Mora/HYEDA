package co.edu.uniminuto.hyeda.modules.auth.service;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import co.edu.uniminuto.hyeda.modules.auth.entity.Permission;
import co.edu.uniminuto.hyeda.modules.auth.entity.Role;
import co.edu.uniminuto.hyeda.modules.auth.entity.UserAccount;
import co.edu.uniminuto.hyeda.modules.auth.repository.PermissionRepository;
import co.edu.uniminuto.hyeda.modules.auth.repository.RoleRepository;
import co.edu.uniminuto.hyeda.modules.auth.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final HyedaProperties properties;

    public DataSeeder(PermissionRepository permissionRepository,
                      RoleRepository roleRepository,
                      UserAccountRepository userAccountRepository,
                      PasswordEncoder passwordEncoder,
                      HyedaProperties properties) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<String[]> perms = List.of(
                new String[]{"INVENTARIO", "Gestion de inventario"},
                new String[]{"ALQUILERES", "Gestion de alquileres"},
                new String[]{"CLIENTES", "Gestion de clientes"},
                new String[]{"FACTURACION", "Pagos y facturacion"},
                new String[]{"DOCUMENTOS", "Documentos PDF"},
                new String[]{"FINANZAS", "Ingresos, gastos y deudas"},
                new String[]{"ANALITICAS", "Dashboard y reportes"},
                new String[]{"ADMIN", "Administracion de usuarios y roles"},
                new String[]{"IMPORTACION", "Importacion de datos"}
        );
        for (String[] p : perms) {
            permissionRepository.findByCode(p[0]).orElseGet(() ->
                    permissionRepository.save(Permission.builder().code(p[0]).description(p[1]).build()));
        }
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() ->
                roleRepository.save(Role.builder()
                        .name("ADMIN")
                        .description("Administrador total")
                        .permissions(Set.copyOf(permissionRepository.findAll()))
                        .build()));
        roleRepository.findByName("EMPLEADO").orElseGet(() -> {
            Set<Permission> emp = Set.of(
                    permissionRepository.findByCode("INVENTARIO").orElseThrow(),
                    permissionRepository.findByCode("ALQUILERES").orElseThrow(),
                    permissionRepository.findByCode("CLIENTES").orElseThrow(),
                    permissionRepository.findByCode("FACTURACION").orElseThrow(),
                    permissionRepository.findByCode("DOCUMENTOS").orElseThrow()
            );
            return roleRepository.save(Role.builder()
                    .name("EMPLEADO")
                    .description("Empleado operativo")
                    .permissions(emp)
                    .build());
        });
        if (!userAccountRepository.existsByUsername(properties.getAdmin().getUsername())) {
            userAccountRepository.save(UserAccount.builder()
                    .username(properties.getAdmin().getUsername())
                    .passwordHash(passwordEncoder.encode(properties.getAdmin().getPassword()))
                    .fullName(properties.getAdmin().getFullName())
                    .email("admin@hyeda.local")
                    .active(true)
                    .roles(Set.of(adminRole))
                    .build());
        }
    }
}
