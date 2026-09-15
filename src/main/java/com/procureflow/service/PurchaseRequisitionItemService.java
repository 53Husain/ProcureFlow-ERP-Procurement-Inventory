package com.procureflow.service;

import com.procureflow.entity.PurchaseRequisitionItem;
import com.procureflow.repository.PurchaseRequisitionItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseRequisitionItemService {

    private final PurchaseRequisitionItemRepository itemRepository;

    public PurchaseRequisitionItemService(
            PurchaseRequisitionItemRepository itemRepository) {

        this.itemRepository = itemRepository;
    }

    // Get all items belonging to a requisition
    public List<PurchaseRequisitionItem> getItemsByRequisitionId(
            Long requisitionId) {

        return itemRepository.findByRequisitionId(requisitionId);
    }

    // Get item by ID
    public PurchaseRequisitionItem getItemById(Long id) {

        return itemRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Requisition item not found with id: " + id));
    }

    // Delete an individual requisition item
    public void deleteItem(Long id) {

        PurchaseRequisitionItem item = getItemById(id);

        itemRepository.delete(item);
    }
}