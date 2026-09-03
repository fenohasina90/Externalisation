package com.cabex.backend.security;

import com.cabex.backend.utilisateur.entity.Utilisateur;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Integer id;
    private final String email;
    private final String password;
    private final boolean active;
    private final Integer roleId;
    private final String roleName;
    private final Set<String> authorities;

    public UserPrincipal(Integer id, String email, String password, boolean active,
                         Integer roleId, String roleName, Set<String> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.active = active;
        this.roleId = roleId;
        this.roleName = roleName;
        this.authorities = authorities;
    }

    public static UserPrincipal from(Utilisateur utilisateur) {
        Set<String> authorities = utilisateur.getRole() != null
                && utilisateur.getRole().getPermissions() != null
                ? utilisateur.getRole().getPermissions().stream()
                        .map(p -> p.getCodePermission())
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        return new UserPrincipal(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getMdpHash(),
                Boolean.TRUE.equals(utilisateur.getActif()),
                utilisateur.getRole() != null ? utilisateur.getRole().getId() : null,
                utilisateur.getRole() != null ? utilisateur.getRole().getNomRole() : null,
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email;
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
        return active;
    }

    public boolean hasPermission(String codePermission) {
        return authorities != null && authorities.contains(codePermission);
    }
}
