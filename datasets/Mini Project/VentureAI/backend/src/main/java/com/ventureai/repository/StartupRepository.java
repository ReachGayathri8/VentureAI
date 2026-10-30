package com.ventureai.repository;

import com.ventureai.entity.Startup;
import com.ventureai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StartupRepository extends JpaRepository<Startup, Long> {
    List<Startup> findByUserOrderByCreatedAtDesc(User user);
    Optional<Startup> findByIdAndUser(Long id, User user);
    long countByUser(User user);
}
