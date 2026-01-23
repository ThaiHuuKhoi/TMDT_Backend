package com.KhoiCG.TMDT.authService.service;

import com.KhoiCG.TMDT.authService.entity.User;
import com.KhoiCG.TMDT.authService.entity.UserPrincipal;
import com.KhoiCG.TMDT.authService.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class MyUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepo repo;


	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// Tìm bằng Email
		User user = repo.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

		return new UserPrincipal(user);
	}

}
