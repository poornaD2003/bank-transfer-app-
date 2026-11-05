
package com.example.bank_transfer_app.config;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final CustomerRepository customerRepository;

    @Bean
    public CommandLineRunner loadSampleData() {

        return args -> {

            if (customerRepository.count() == 0) {

                Customer c1 = new Customer();
                c1.setAccountNumber("100001");
                c1.setFullName("Kamal");
                c1.setEmail("kamal@gmail.com");
                c1.setBalance(new BigDecimal("50000.00"));
                customerRepository.save(c1);

                Customer c2 = new Customer();
                c2.setAccountNumber("100002");
                c2.setFullName("Nimali");
                c2.setEmail("nimali@gmail.com");
                c2.setBalance(new BigDecimal("25000.00"));
                customerRepository.save(c2);

                Customer c3 = new Customer();
                c3.setAccountNumber("100003");
                c3.setFullName("Saman");
                c3.setEmail("saman@gmail.com");
                c3.setBalance(new BigDecimal("10000.00"));
                customerRepository.save(c3);

                System.out.println("Sample customers inserted successfully!");

            } else {
                System.out.println("Customers already exist. Skipping sample data.");
            }

            if (customerRepository.countAccounts("999999") == 0) {

                Customer bank = new Customer();

                bank.setAccountNumber("999999");
                bank.setFullName("Bank Fee Account");
                bank.setEmail("bank@gmail.com");
                bank.setBalance(BigDecimal.ZERO);

                customerRepository.save(bank);

                System.out.println("Bank fee account created successfully!");

            } else {
                System.out.println("Bank fee account already exists.");
            }
        };
    }
}
