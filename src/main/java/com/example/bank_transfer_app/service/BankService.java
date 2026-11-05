package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.model.*;
import com.example.bank_transfer_app.repository.*;
import com.example.bank_transfer_app.exception.*;

import lombok.RequiredArgsConstructor;

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

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) {

        if (fromAccount.equals(toAccount)) {
            throw new InvalidTransferException(
                    "Accounts must be different accounts");
        }

        Customer sender = customerRepository
                .getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Sender account not found"));

        customerRepository
                .getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Receiver account not found"));

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException(
                    "Amount must be greater than zero");
        }

        int debited = customerRepository.debit(
                fromAccount, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        if (simulateFailure) {
            throw new RuntimeException(
                    "All changes were rolled back");
        }

        int credited = customerRepository.credit(
                toAccount, amount);

        if (credited == 0) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Transfer successful");

        return transactionRepository.save(transaction);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        return customerRepository.searchByName(keyword);
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();

        return total == null ? BigDecimal.ZERO : total;
    }

    // Get latest transactions
    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(
            String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(
            BigDecimal min) {

        return transactionRepository.findLargeTransfers(min);
    }

    public BigDecimal getTotalSent(String account) {

        BigDecimal total = transactionRepository.getTotalSent(
                account,
                TransactionStatus.SUCCESS);

        return total == null ? BigDecimal.ZERO : total;
    }

    public long getSuccessCount() {
        return transactionRepository.countByStatus(
                TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return transactionRepository.countByStatus(
                TransactionStatus.FAILED);
    }

    public long getCustomerCount() {
        return customerRepository.countCustomersNative();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(String account, BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransferException(
                    "Deposit amount must be greater than zero");
        }

        customerRepository.getByAccountNumber(account)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"));

        int updated = customerRepository.credit(account, amount);

        if (updated == 0) {
            throw new AccountNotFoundException(
                    "Account not found");
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount("CASH_DEPOSIT");
        transaction.setToAccount(account);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Cash deposit");

        transactionRepository.save(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String account, BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransferException(
                    "Withdrawal amount must be greater than zero");
        }

        customerRepository.getByAccountNumber(account)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found"));

        int updated = customerRepository.debit(account, amount);

        if (updated == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(account);
        transaction.setToAccount("CASH_WITHDRAWAL");
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Cash withdrawal");

        transactionRepository.save(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferWithFee(
            String fromAccount,
            String toAccount,
            BigDecimal amount) {

        if (fromAccount.equals(toAccount)) {
            throw new InvalidTransferException(
                    "Accounts must be different");
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException(
                    "Transfer amount must be greater than zero");
        }

        customerRepository.getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Sender account not found"));

        customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Receiver account not found"));

        customerRepository.getByAccountNumber("999999")
                .orElseThrow(() -> new AccountNotFoundException(
                        "Bank fee account not found"));

        BigDecimal fee = amount.multiply(
                new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal totalDebit = amount.add(fee);

        int debitResult = customerRepository.debit(
                fromAccount, totalDebit);

        if (debitResult == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance including bank fee");
        }

        int creditReceiver = customerRepository.credit(
                toAccount, amount);

        if (creditReceiver == 0) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        int creditBank = customerRepository.credit(
                "999999", fee);

        if (creditBank == 0) {
            throw new AccountNotFoundException(
                    "Bank fee account not found");
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);

        transaction.setDescription(
                "Transfer successful. Bank fee: LKR " + fee);

        transactionRepository.save(transaction);
    }
}