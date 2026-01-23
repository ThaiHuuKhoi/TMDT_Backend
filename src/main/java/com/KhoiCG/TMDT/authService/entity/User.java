package com.KhoiCG.TMDT.authService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Table(name = "users")
@Entity
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true) // Email không được trùng
	private String email;

	private String password;
	private String name;
	private String role;

	@Enumerated(EnumType.STRING)
	private AuthProvider provider;

	private String providerId;
}
