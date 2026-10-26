package com.procureflow.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ReceiptConflictException extends IllegalStateException {
    public ReceiptConflictException(String message) {
        super(message);
    }
}
