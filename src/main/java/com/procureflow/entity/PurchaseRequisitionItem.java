package com.procureflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_requisition_items")
public class PurchaseRequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private PurchaseRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 255)
    private String itemRemarks;

    public PurchaseRequisitionItem() {
    }

    public Long getId() {
        return id;
    }

    public PurchaseRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(PurchaseRequisition requisition) {
        this.requisition = requisition;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getItemRemarks() {
        return itemRemarks;
    }

    public void setItemRemarks(String itemRemarks) {
        this.itemRemarks = itemRemarks;
    }
}