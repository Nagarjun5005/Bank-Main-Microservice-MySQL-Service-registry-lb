package com.nagarjun.Account.service;

import com.nagarjun.Account.dto.CustomerDetailsDto;
import com.nagarjun.Account.dto.CustomerDto;

public interface CustomerService {

    public CustomerDetailsDto getCustomerDetails(String mobileNumber);

}
