package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.entity.UserPrincipal;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // 1. Tự động tạo constructor cho userRepo
public class MyUserDetailsService implements UserDetailsService {

	private final UserRepo userRepo;

	@Override
	@Transactional // 2. Giữ kết nối DB để tránh lỗi Lazy Loading khi map sang UserPrincipal
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepo.findByEmail(email)
				.map(UserPrincipal::new) // 3. Code gọn hơn: Nếu tìm thấy User -> New UserPrincipal(user)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
	}
}