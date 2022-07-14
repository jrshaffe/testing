package com.example.api;

import com.example.data.Customer;
import com.example.data.CustomerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class CustomerController {

	private CustomerRepository customerRepository;

	@GetMapping("/customers")
	public @ResponseBody Iterable<Customer> getCustomers() {
		return customerRepository.findAll();
	}

	@Autowired
	public void setCustomerRepository(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

}
