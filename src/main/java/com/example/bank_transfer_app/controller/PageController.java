package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.service.BankService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final BankService bankService;

    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {

        model.addAttribute(
                "transactions",
                bankService.getLargeTransfers(min));

        model.addAttribute("min", min);

        return "large-transfers";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(
            @PathVariable String accountNumber,
            Model model) {

        model.addAttribute(
                "transactions",
                bankService.getAccountHistory(accountNumber));

        model.addAttribute("accountNumber", accountNumber);

        model.addAttribute(
                "totalSent",
                bankService.getTotalSent(accountNumber));

        return "history";
    }
}
