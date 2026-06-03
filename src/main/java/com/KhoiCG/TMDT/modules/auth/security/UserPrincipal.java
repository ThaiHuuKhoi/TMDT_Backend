package com.KhoiCG.TMDT.modules.auth.security;

import com.KhoiCG.TMDT.modules.user.entity.AuthProvider;
import com.KhoiCG.TMDT.modules.user.entity.Role;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.entity.UserProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
	private User user;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Role role = user.getRole();
		String roleName = (role != null) ? role.name() : Role.USER.name();
		return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
	}

	@Override
	public String getPassword() {
		return user.getProviders().stream()
				.filter(p -> p.getProvider() == AuthProvider.LOCAL)
				.map(UserProvider::getPasswordHash)
				.findFirst()
				.orElse(null);
	}

	@Override
	public String getUsername() {
		return user.getEmail();
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
		return Boolean.TRUE.equals(user.getIsActive());
	}
}
