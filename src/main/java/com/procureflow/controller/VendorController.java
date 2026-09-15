package com.procureflow.controller;

import com.procureflow.entity.Vendor;
import com.procureflow.service.VendorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    // Create vendor
    @PostMapping
    public ResponseEntity<Vendor> createVendor(
            @RequestBody Vendor vendor) {

        Vendor createdVendor = vendorService.createVendor(vendor);

        return new ResponseEntity<>(createdVendor, HttpStatus.CREATED);
    }

    // Get all vendors
    @GetMapping
    public ResponseEntity<List<Vendor>> getAllVendors() {

        return ResponseEntity.ok(
                vendorService.getAllVendors()
        );
    }

    // Get vendor by ID
    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                vendorService.getVendorById(id)
        );
    }

    // Update vendor
    @PutMapping("/{id}")
    public ResponseEntity<Vendor> updateVendor(
            @PathVariable Long id,
            @RequestBody Vendor vendor) {

        return ResponseEntity.ok(
                vendorService.updateVendor(id, vendor)
        );
    }

    // Delete vendor
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(
            @PathVariable Long id) {

        vendorService.deleteVendor(id);

        return ResponseEntity.noContent().build();
    }
}