package com.KhoiCG.TMDT.authService.entity;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	private User user;

	public UserPrincipal(User user) {
this.user=user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Nếu role trong DB null thì default là USER, ngược lại lấy role thực tế
		String roleName = (user.getRole() == null) ? "USER" : user.getRole();
		return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + roleName));
	}
	@Override
	public String getPassword() {
		
		return user.getPassword();
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
		return true;
	}

}
