package com.KhoiCG.TMDT.authService.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder // Giúp tạo object User dễ dàng: User.builder().name("A").build()
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	private String password; // Null nếu login bằng Google/Facebook

	private String name;

	private String role; // Ví dụ: "USER", "ADMIN"

	@Enumerated(EnumType.STRING)
	private AuthProvider provider;

	private String providerId; // ID từ Google/Facebook trả về
}