package com.procureflow.entity;
import com.procureflow.enums.RequisitionStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_requisitions")
public class PurchaseRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String requisitionNumber;

    @Column(nullable = false, length = 100)
    private String requestedBy;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false)
    private LocalDateTime requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequisitionStatus status;

    @Column(length = 500)
    private String remarks;

    @OneToMany(
            mappedBy = "requisition",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PurchaseRequisitionItem> items;

    public PurchaseRequisition() {
    }

    public Long getId() {
        return id;
    }

    public String getRequisitionNumber() {
        return requisitionNumber;
    }

    public void setRequisitionNumber(String requisitionNumber) {
        this.requisitionNumber = requisitionNumber;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDateTime requestedDate) {
        this.requestedDate = requestedDate;
    }

    public RequisitionStatus getStatus() {
        return status;
    }

    public void setStatus(RequisitionStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<PurchaseRequisitionItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseRequisitionItem> items) {
        this.items = items;
    }
}