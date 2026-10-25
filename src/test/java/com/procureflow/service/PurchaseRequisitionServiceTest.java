package com.procureflow.service;

import com.procureflow.entity.*;
import com.procureflow.enums.RequisitionStatus;
import com.procureflow.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseRequisitionServiceTest {
    private final PurchaseRequisitionRepository repository = mock(PurchaseRequisitionRepository.class);
    private final MaterialRepository materials = mock(MaterialRepository.class);
    private final PurchaseRequisitionService service = new PurchaseRequisitionService(repository, materials);

    private PurchaseRequisition request(Integer quantity) {
        Material material = new Material();
        ReflectionTestUtils.setField(material, "id", 10L);
        PurchaseRequisitionItem item = new PurchaseRequisitionItem();
        item.setMaterial(material); item.setQuantity(quantity);
        PurchaseRequisition request = new PurchaseRequisition();
        request.setRequisitionNumber("REQ-1");
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void createsPendingRequisitionAndResolvesMaterial() {
        var request = request(3);
        Material stored = new Material();
        when(materials.findById(10L)).thenReturn(Optional.of(stored));
        when(repository.save(any(PurchaseRequisition.class))).thenAnswer(i -> i.getArgument(0));
        request.setStatus(RequisitionStatus.APPROVED);
        var result = service.createRequisition(request);
        assertEquals(RequisitionStatus.PENDING_APPROVAL, result.getStatus());
        assertNotNull(result.getRequestedDate());
        assertSame(stored, result.getItems().get(0).getMaterial());
        assertSame(result, result.getItems().get(0).getRequisition());
        verify(repository).save(result);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rejectsInvalidQuantitiesWithoutSaving(Integer quantity) {
        assertThrows(IllegalArgumentException.class, () -> service.createRequisition(request(quantity)));
        verify(repository, never()).save(any());
        verifyNoInteractions(materials);
    }

    @Test
    void rejectsDuplicateNumber() {
        when(repository.existsByRequisitionNumber("REQ-1")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.createRequisition(request(2)));
        verify(repository, never()).save(any());
        verifyNoInteractions(materials);
    }

    @Test
    void rejectsEmptyItems() {
        var request = request(2); request.setItems(List.of());
        assertThrows(IllegalArgumentException.class, () -> service.createRequisition(request));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnknownMaterialWithoutSaving() {
        when(materials.findById(10L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.createRequisition(request(2)));
        verify(repository, never()).save(any());
    }

    @Test
    void approvesPendingRequest() {
        var request = request(2); request.setStatus(RequisitionStatus.PENDING_APPROVAL);
        when(repository.findById(1L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);
        assertEquals(RequisitionStatus.APPROVED, service.approveRequisition(1L).getStatus());
    }

    @Test
    void rejectsPendingRequest() {
        var request = request(2); request.setStatus(RequisitionStatus.PENDING_APPROVAL);
        when(repository.findById(1L)).thenReturn(Optional.of(request));
        when(repository.save(request)).thenReturn(request);
        assertEquals(RequisitionStatus.REJECTED, service.rejectRequisition(1L).getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = RequisitionStatus.class, names = "PENDING_APPROVAL", mode = EnumSource.Mode.EXCLUDE)
    void preventsDecisionsOnNonPendingRequest(RequisitionStatus status) {
        var request = request(2); request.setStatus(status);
        when(repository.findById(1L)).thenReturn(Optional.of(request));
        assertThrows(IllegalStateException.class, () -> service.approveRequisition(1L));
        assertThrows(IllegalStateException.class, () -> service.rejectRequisition(1L));
        assertEquals(status, request.getStatus());
        verify(repository, never()).save(any());
    }
}
