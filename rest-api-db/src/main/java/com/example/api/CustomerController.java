package com.example.api;

import javax.transaction.Transactional;

import com.example.data.Customer;
import com.example.data.CustomerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@Transactional
public class CustomerController {

	private CustomerRepository customerRepository;

	@GetMapping(value = "/customers", produces = MediaType.APPLICATION_JSON_VALUE)
	public Iterable<Customer> getCustomers() {
		return customerRepository.findAll();
	}

	@GetMapping(value = "/customers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Customer getCustomersById(@PathVariable String id) {
		return customerRepository.findById(Long.parseLong(id));
	}

	@PostMapping(value = "/customers", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Customer postCustomer(@RequestBody Customer in) {
		Customer customer = new Customer(in.getFirstName(), in.getLastName());
		return customerRepository.save(customer);
	}

	@PutMapping(value = "/customers/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Customer createPerson(@PathVariable String id, @RequestBody Customer in) {
		Customer customer = customerRepository.findById(Long.parseLong(id));
		if (customer == null) {
			throw new DataRetrievalFailureException("Customer with id '" + id + "' not found.");
		}
		customer.setFirstName(in.getFirstName());
		customer.setLastName(in.getLastName());
		return customerRepository.save(customer);
	}

	@DeleteMapping("/customers/{id}")
	public void deleteCustomersById(@PathVariable String id) {
		customerRepository.deleteById(Long.parseLong(id));
	}

	@Autowired
	public void setCustomerRepository(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

}
