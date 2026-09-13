package com.nagarjun.Account.service.impl;

import com.nagarjun.Account.dto.*;
import com.nagarjun.Account.entity.Account;
import com.nagarjun.Account.entity.Customer;
import com.nagarjun.Account.exception.ResourceNotFoundException;
import com.nagarjun.Account.mapper.AccountMapper;
import com.nagarjun.Account.mapper.CustomerMapper;
import com.nagarjun.Account.repository.AccountsRepository;
import com.nagarjun.Account.repository.CustomerRepository;
import com.nagarjun.Account.service.CustomerService;
import com.nagarjun.Account.service.feignclient.CardsFeignClient;
import com.nagarjun.Account.service.feignclient.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private CustomerRepository customerRepository;

    private AccountsRepository accountsRepository;

    private CardsFeignClient cardsFeignClient;

    private LoansFeignClient loansFeignClient;

    @Override
    public CustomerDetailsDto getCustomerDetails(String mobileNumber) {

        //find the customer using findByMobileNumber
        Customer customer = customerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        // find the account using findByCustomerId
        Account account = accountsRepository.findByCustomerId(customer.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString()));


        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountMapper.mapToAccountsDto(account, new AccountsDto()));

        //similarly i need to set the cards and loans to this customer details

        ResponseEntity<CardDto> cardDtoResponseEntity = cardsFeignClient.fetchCardDetails(mobileNumber);
        customerDetailsDto.setCardDto(cardDtoResponseEntity.getBody());

        ResponseEntity<LoanDto> loanDtoResponseEntity = loansFeignClient.fetchLoanDetails(mobileNumber);
        customerDetailsDto.setLoanDto(loanDtoResponseEntity.getBody());

        return customerDetailsDto;

    }
}
