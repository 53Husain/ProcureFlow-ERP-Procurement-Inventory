package com.procureflow.service;

import com.procureflow.entity.Vendor;
import com.procureflow.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    // Create a new vendor
    public Vendor createVendor(Vendor vendor) {

        if (vendorRepository.existsByVendorCode(vendor.getVendorCode())) {
            throw new IllegalArgumentException("Vendor code already exists");
        }

        if (vendor.getActive() == null) {
            vendor.setActive(true);
        }

        return vendorRepository.save(vendor);
    }

    // Get all vendors
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    // Get vendor by ID
    public Vendor getVendorById(Long id) {

        return vendorRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found with id: " + id));
    }

    // Update vendor
    public Vendor updateVendor(Long id, Vendor updatedVendor) {

        Vendor existingVendor = getVendorById(id);

        existingVendor.setVendorName(updatedVendor.getVendorName());
        existingVendor.setContactPerson(updatedVendor.getContactPerson());
        existingVendor.setEmail(updatedVendor.getEmail());
        existingVendor.setPhone(updatedVendor.getPhone());
        existingVendor.setAddress(updatedVendor.getAddress());
        existingVendor.setCity(updatedVendor.getCity());
        existingVendor.setCountry(updatedVendor.getCountry());
        existingVendor.setActive(updatedVendor.getActive());

        return vendorRepository.save(existingVendor);
    }

    // Delete vendor
    public void deleteVendor(Long id) {

        Vendor vendor = getVendorById(id);

        vendorRepository.delete(vendor);
    }
}