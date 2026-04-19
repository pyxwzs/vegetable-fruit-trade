package com.trade.service;

import com.trade.dto.CreditWarningDTO;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;

    public Page<Customer> getCustomers(String keyword, String creditLevel, Pageable pageable) {
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("name"), kw),
                        cb.like(root.get("customerCode"), kw),
                        cb.like(root.get("contact"), kw)
                ));
            }
            if (creditLevel != null && !creditLevel.isBlank()) {
                ps.add(cb.equal(root.get("creditLevel"), Customer.CreditLevel.valueOf(creditLevel.trim().toUpperCase())));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return customerRepository.findAll(spec, pageable);
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("客户不存在"));
    }

    /**
     * 授信额度 &gt; 0 的客户：未结清占用达到额度 80% 以上时预警（含超额）。
     */
    public List<CreditWarningDTO> listCreditWarnings() {
        List<CreditWarningDTO> out = new ArrayList<>();
        for (Customer c : customerRepository.findByStatusOrderByIdAsc(Customer.CustomerStatus.ACTIVE)) {
            BigDecimal limit = c.getCreditLimit() != null ? c.getCreditLimit() : BigDecimal.ZERO;
            if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal used = salesOrderRepository.sumUnpaidExposure(c.getId());
            BigDecimal ratio = used.divide(limit, 4, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.8")) >= 0) {
                boolean over = used.compareTo(limit) > 0;
                out.add(new CreditWarningDTO(c.getId(), c.getName(), limit, used, over));
            }
        }
        return out;
    }

    @Transactional
    public Customer create(CustomerDTO dto) {
        if (customerRepository.existsByCustomerCode(dto.getCustomerCode().trim())) {
            throw new BusinessException("客户编码已存在");
        }
        Customer c = new Customer();
        fillFromDto(c, dto, true);
        return customerRepository.save(c);
    }

    @Transactional
    public Customer update(Long id, CustomerDTO dto) {
        Customer c = getById(id);
        if (!c.getCustomerCode().equals(dto.getCustomerCode().trim())
                && customerRepository.existsByCustomerCode(dto.getCustomerCode().trim())) {
            throw new BusinessException("客户编码已存在");
        }
        fillFromDto(c, dto, false);
        return customerRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new BusinessException("客户不存在");
        }
        customerRepository.deleteById(id);
    }

    private void fillFromDto(Customer c, CustomerDTO dto, boolean creating) {
        c.setCustomerCode(dto.getCustomerCode().trim());
        c.setName(dto.getName().trim());
        c.setContact(emptyToNull(dto.getContact()));
        c.setPhone(emptyToNull(dto.getPhone()));
        c.setEmail(emptyToNull(dto.getEmail()));
        c.setAddress(emptyToNull(dto.getAddress()));
        c.setTaxNumber(emptyToNull(dto.getTaxNumber()));
        if (dto.getType() != null && !dto.getType().isBlank()) {
            c.setType(Customer.CustomerType.valueOf(dto.getType().trim().toUpperCase()));
        } else if (creating) {
            c.setType(Customer.CustomerType.RETAIL);
        }
        if (dto.getCreditLevel() != null && !dto.getCreditLevel().isBlank()) {
            c.setCreditLevel(Customer.CreditLevel.valueOf(dto.getCreditLevel().trim().toUpperCase()));
        } else if (creating) {
            c.setCreditLevel(Customer.CreditLevel.B);
        }
        c.setCreditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO);
        if (dto.getTotalPurchaseAmount() != null) {
            c.setTotalPurchaseAmount(dto.getTotalPurchaseAmount());
        } else if (creating) {
            c.setTotalPurchaseAmount(BigDecimal.ZERO);
        }
        if (dto.getPurchaseCount() != null) {
            c.setPurchaseCount(dto.getPurchaseCount());
        } else if (creating) {
            c.setPurchaseCount(0);
        }
        c.setRemark(emptyToNull(dto.getRemark()));
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            c.setStatus(Customer.CustomerStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        } else if (creating) {
            c.setStatus(Customer.CustomerStatus.ACTIVE);
        }
    }

    private static String emptyToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
