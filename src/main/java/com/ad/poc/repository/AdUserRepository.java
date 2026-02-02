package com.ad.poc.repository;

import com.ad.poc.entity.AdUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdUserRepository extends JpaRepository<AdUser, Long> {

    Optional<AdUser> findBySamAccountName(String samAccountName);

    Optional<AdUser> findByEmail(String email);

    List<AdUser> findByDepartment(String department);

    List<AdUser> findByEnabled(boolean enabled);

    boolean existsBySamAccountName(String samAccountName);
}
