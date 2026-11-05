package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.service.BankService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final BankService bankService;

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        model.addAttribute(
                "customers",
                bankService.searchCustomers(keyword));

        model.addAttribute(
                "totalBalance",
                bankService.getTotalBalance());

        model.addAttribute(
                "customerCount",
                bankService.getCustomerCount());

        model.addAttribute(
                "successCount",
                bankService.getSuccessCount());

        model.addAttribute(
                "failedCount",
                bankService.getFailedCount());

        model.addAttribute(
                "transactions",
                bankService.getLatestTransactions());

        model.addAttribute("customer", new Customer());

        model.addAttribute("transferRequest", new TransferRequest());

        return "bank";
    }

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

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {

            bankService.deposit(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Deposit successful!");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {

            bankService.withdraw(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Withdrawal successful!");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/transfer-with-fee")
public String transferWithFee(
        @RequestParam String fromAccount,
        @RequestParam String toAccount,
        @RequestParam BigDecimal amount,
        RedirectAttributes redirectAttributes) {

    try {

        bankService.transferWithFee(
                fromAccount,
                toAccount,
                amount
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Transfer completed successfully with 1% bank fee!"
        );

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                e.getMessage()
        );
    }

    return "redirect:/";
}
}
