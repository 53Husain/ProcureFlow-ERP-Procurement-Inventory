package com.procureflow.service;

import com.procureflow.entity.Material;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MaterialStockLock {
    private final EntityManager entityManager;

    public MaterialStockLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // The caller must keep this lock through its entire stock update and commit.
    @Transactional(propagation = Propagation.MANDATORY)
    public Material lock(Long id) {
        // Preserve earlier service updates if the caller groups several receipts
        // in one transaction; refresh must not discard pending managed changes.
        entityManager.flush();
        Material material = entityManager.find(Material.class, id);
        if (material == null) {
            throw new IllegalArgumentException("Material not found with id: " + id);
        }
        // A lock alone can leave an already-managed entity with stale stock.
        // Refresh under FOR UPDATE reads the latest committed row, including
        // when this MySQL REPEATABLE READ transaction has an older snapshot.
        entityManager.refresh(material, LockModeType.PESSIMISTIC_WRITE);
        return material;
    }
}
