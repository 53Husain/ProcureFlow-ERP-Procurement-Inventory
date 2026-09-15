package com.procureflow.service;

import com.procureflow.entity.Material;
import com.procureflow.repository.MaterialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    // Create a new material
    public Material createMaterial(Material material) {

        if (materialRepository.existsByMaterialCode(material.getMaterialCode())) {
            throw new IllegalArgumentException("Material code already exists");
        }

        if (material.getCurrentStock() == null) {
            material.setCurrentStock(0);
        }

        if (material.getActive() == null) {
            material.setActive(true);
        }

        if (material.getReorderLevel() == null || material.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }

        if (material.getUnitPrice() == null || material.getUnitPrice().signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }

        return materialRepository.save(material);
    }

    // Get all materials
    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    // Get material by ID
    public Material getMaterialById(Long id) {

        return materialRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Material not found with id: " + id));
    }

    // Update an existing material
    public Material updateMaterial(Long id, Material updatedMaterial) {

        Material existingMaterial = getMaterialById(id);

        existingMaterial.setMaterialName(updatedMaterial.getMaterialName());
        existingMaterial.setCategory(updatedMaterial.getCategory());
        existingMaterial.setUnitOfMeasure(updatedMaterial.getUnitOfMeasure());
        existingMaterial.setUnitPrice(updatedMaterial.getUnitPrice());
        existingMaterial.setCurrentStock(updatedMaterial.getCurrentStock());
        existingMaterial.setReorderLevel(updatedMaterial.getReorderLevel());
        existingMaterial.setActive(updatedMaterial.getActive());

        return materialRepository.save(existingMaterial);
    }

    // Delete material
    public void deleteMaterial(Long id) {

        Material material = getMaterialById(id);

        materialRepository.delete(material);
    }

    // Get materials that need restocking
    public List<Material> getLowStockMaterials() {

        return materialRepository.findAll()
                .stream()
                .filter(material ->
                        material.getCurrentStock() <= material.getReorderLevel())
                .toList();
    }
}
