package com.nagarjun.Account.controller;


import com.nagarjun.Account.dto.CustomerDetailsDto;
import com.nagarjun.Account.dto.CustomerDto;
import com.nagarjun.Account.service.CustomerService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService){
        this.customerService=customerService;
    }

    @GetMapping("/getCustomerDetails")
    public ResponseEntity<CustomerDetailsDto>getCustomerDetails(@RequestParam
                                                         @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
                                                         String mobileNumber){

        //business logic
        CustomerDetailsDto customerDetails = customerService.getCustomerDetails(mobileNumber);
        return ResponseEntity.status(HttpStatus.OK).body(customerDetails);

    }
}
