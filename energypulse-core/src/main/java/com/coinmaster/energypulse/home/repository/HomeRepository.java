package com.coinmaster.energypulse.home.repository;

import com.coinmaster.energypulse.home.domain.Home;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HomeRepository extends JpaRepository<Home, UUID> {
}