package com.KhoiCG.TMDT.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
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

	private String name;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private Role role = Role.USER;

	@Builder.Default
	private Boolean isActive = true;

	@Column(name = "avatar")
	private String avatar;

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<UserProvider> providers = new ArrayList<>();

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	public List<UserProvider> getProviders() {
		if (this.providers == null) {
			this.providers = new ArrayList<>();
		}
		return this.providers;
	}
}
