package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.model.*;
import com.example.bank_transfer_app.repository.*;
import com.example.bank_transfer_app.exception.*;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;

    private final BankTransactionRepository transactionRepository;

    // Create customer
    // Create customer
    @Transactional
    public Customer createCustomer(Customer customer) {

        if (customerRepository.countAccounts(
                customer.getAccountNumber()) > 0) {
            throw new InvalidTransferException(
                    "Account number already exists");
        }

        if (customerRepository.countEmails(
                customer.getEmail()) > 0) {
            throw new InvalidTransferException(
                    "Email already exists");
        }

        return customerRepository.save(customer);
    }

    // Transfer money
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) {

        // Check different accounts
        if (fromAccount.equals(toAccount)) {
            throw new InvalidTransferException(
                    "Accounts must be different accounts");
        }

        // Check sender exists
        Customer sender = customerRepository
                .getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Sender account not found"));

        // Check receiver exists
        customerRepository
                .getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Receiver account not found"));

        // Check amount
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException(
                    "Amount must be greater than zero");
        }

        // Withdraw money
        int debited = customerRepository.debit(
                fromAccount, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        // Simulate system failure
        if (simulateFailure) {
            throw new RuntimeException(
                    "All changes were rolled back");
        }

        // Deposit money
        int credited = customerRepository.credit(
                toAccount, amount);

        if (credited == 0) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        // Save successful transaction
        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Transfer successful");

        return transactionRepository.save(transaction);
    }

    // Find all customers
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    // Search customers
    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        return customerRepository.searchByName(keyword);
    }

    // Get total balance
    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();

        return total == null ? BigDecimal.ZERO : total;
    }

    // Get latest transactions
    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    // Get account history
    public List<BankTransaction> getAccountHistory(
            String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    // Transaction statistics
    public long getSuccessCount() {
        return transactionRepository.countByStatus(
                TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return transactionRepository.countByStatus(
                TransactionStatus.FAILED);
    }

    // Count customers using native SQL
    public long getCustomerCount() {
        return customerRepository.countCustomersNative();
    }

    public @Nullable Object getTotalSent(String accountNumber) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalSent'");
    }
}