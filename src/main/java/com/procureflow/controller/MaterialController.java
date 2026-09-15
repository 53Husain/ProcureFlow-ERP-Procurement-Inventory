package com.procureflow.controller;

import com.procureflow.entity.Material;
import com.procureflow.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    // Create material
    @PostMapping
    public ResponseEntity<Material> createMaterial(
            @RequestBody Material material) {

        Material createdMaterial = materialService.createMaterial(material);

        return new ResponseEntity<>(createdMaterial, HttpStatus.CREATED);
    }

    // Get all materials
    @GetMapping
    public ResponseEntity<List<Material>> getAllMaterials() {

        return ResponseEntity.ok(
                materialService.getAllMaterials()
        );
    }

    // Get material by ID
    @GetMapping("/{id}")
    public ResponseEntity<Material> getMaterialById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                materialService.getMaterialById(id)
        );
    }

    // Update material
    @PutMapping("/{id}")
    public ResponseEntity<Material> updateMaterial(
            @PathVariable Long id,
            @RequestBody Material material) {

        return ResponseEntity.ok(
                materialService.updateMaterial(id, material)
        );
    }

    // Delete material
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long id) {

        materialService.deleteMaterial(id);

        return ResponseEntity.noContent().build();
    }

    // Get low-stock materials
    @GetMapping("/low-stock")
    public ResponseEntity<List<Material>> getLowStockMaterials() {

        return ResponseEntity.ok(
                materialService.getLowStockMaterials()
        );
    }
}
