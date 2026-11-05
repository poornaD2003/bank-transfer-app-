package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BankController {

        private final BankService bankService;
        private final TransactionAuditService auditService;

        @GetMapping("/customers")
        public ResponseEntity<?> getCustomers(
                        @RequestParam(required = false) String keyword) {

                return ResponseEntity.ok(
                                bankService.searchCustomers(keyword));
        }

        @PostMapping("/customers")
        public ResponseEntity<?> addCustomer(
                        @RequestBody Customer customer) {

                try {
                        Customer savedCustomer = bankService.createCustomer(customer);

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(savedCustomer);

                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        @PostMapping("/transfers")
        public ResponseEntity<?> transfer(
                        @Valid @RequestBody TransferRequest request) {

                try {
                        bankService.transfer(
                                        request.getFromAccount(),
                                        request.getToAccount(),
                                        request.getAmount(),
                                        request.isSimulateFailure());

                        return ResponseEntity.ok(
                                        Map.of("message", "Transfer completed successfully"));

                } catch (RuntimeException e) {

                        try {
                                auditService.recordFailure(
                                                request.getFromAccount(),
                                                request.getToAccount(),
                                                request.getAmount(),
                                                e.getMessage());
                        } catch (RuntimeException auditError) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of(
                                                                "error",
                                                                "Transfer failed and audit record could not be saved"));
                        }

                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/accounts/{accountNumber}/history")
        public String history(
                        @PathVariable String accountNumber,
                        Model model) {

                model.addAttribute(
                                "transactions",
                                bankService.getAccountHistory(accountNumber));

                model.addAttribute(
                                "accountNumber",
                                accountNumber);

                model.addAttribute(
                                "totalSent",
                                bankService.getTotalSent(accountNumber));

                return "history";
        }

}