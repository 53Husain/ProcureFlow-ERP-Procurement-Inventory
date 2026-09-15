package com.procureflow.repository;

import com.procureflow.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByMaterialCode(String materialCode);

    boolean existsByMaterialCode(String materialCode);
}