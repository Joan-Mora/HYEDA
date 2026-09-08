package co.edu.uniminuto.hyeda.iam.security;

import co.edu.uniminuto.hyeda.iam.domain.UserAccount;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Principal de Spring Security.
 *
 * <p>Las autoridades son los codigos de permiso tal cual (por ejemplo
 * {@code ALQUILERES_GESTIONAR}) mas los roles con prefijo {@code ROLE_}. Los
 * controladores autorizan siempre por permiso, nunca por rol, para que crear un
 * rol nuevo desde la interfaz no exija tocar codigo.
 */
@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final boolean mustChangePassword;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Collection<GrantedAuthority> authorities;

    public AuthenticatedUser(UserAccount account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.password = account.getPasswordHash();
        this.fullName = account.getFullName();
        this.active = Boolean.TRUE.equals(account.getActive());
        this.mustChangePassword = Boolean.TRUE.equals(account.getMustChangePassword());
        this.roles = account.roleNames();
        this.permissions = account.permissionCodes();

        Set<GrantedAuthority> granted = new LinkedHashSet<>();
        permissions.forEach(code -> granted.add(new SimpleGrantedAuthority(code)));
        roles.forEach(role -> granted.add(new SimpleGrantedAuthority("ROLE_" + role)));
        this.authorities = Set.copyOf(granted);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
