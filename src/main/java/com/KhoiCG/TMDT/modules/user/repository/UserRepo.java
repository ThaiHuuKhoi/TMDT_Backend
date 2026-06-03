package com.KhoiCG.TMDT.modules.user.repository;

import com.KhoiCG.TMDT.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);

	// Dùng cho authentication — load providers trong cùng query để tránh LazyInitializationException
	@EntityGraph(attributePaths = {"providers"})
	@Query("SELECT u FROM User u WHERE u.email = :email")
	Optional<User> findWithProvidersByEmail(@Param("email") String email);

	Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name, Pageable pageable);
	Page<User> findByIsActiveAndEmailContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCase(
			Boolean isActiveForEmail,
			String email,
			Boolean isActiveForName,
			String name,
			Pageable pageable
	);
}
