package com.trade.service;

import com.trade.dto.CustomerDTO;
import com.trade.entity.Customer;
import com.trade.exception.BusinessException;
import com.trade.repository.CustomerRepository;
import com.trade.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;

    public Page<Customer> getCustomers(String keyword, Pageable pageable) {
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("name"), kw),
                        cb.like(root.get("customerCode"), kw),
                        cb.like(root.get("contact"), kw),
                        cb.like(root.get("phone"), kw)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return customerRepository.findAll(spec, pageable);
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("客户不存在"));
    }

    @Transactional
    public Customer create(CustomerDTO dto) {
        Customer c = new Customer();
        c.setCustomerCode("CST" + System.currentTimeMillis());
        fillFromDto(c, dto);
        return customerRepository.save(c);
    }

    @Transactional
    public Customer update(Long id, CustomerDTO dto) {
        Customer c = getById(id);
        fillFromDto(c, dto);
        return customerRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new BusinessException("客户不存在");
        }
        if (salesOrderRepository.existsByCustomer_Id(id)) {
            throw new BusinessException("该客户已关联销售订单，无法删除。可将客户状态设为停用。");
        }
        customerRepository.deleteById(id);
    }

    private void fillFromDto(Customer c, CustomerDTO dto) {
        c.setName(dto.getName().trim());
        c.setContact(emptyToNull(dto.getContact()));
        c.setPhone(emptyToNull(dto.getPhone()));
        c.setAddress(emptyToNull(dto.getAddress()));
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            c.setStatus(Customer.CustomerStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        } else if (c.getId() == null) {
            c.setStatus(Customer.CustomerStatus.ACTIVE);
        }
    }

    private static String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
