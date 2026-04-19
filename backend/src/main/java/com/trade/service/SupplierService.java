package com.trade.service;

import com.trade.dto.SupplierDTO;
import com.trade.entity.Supplier;
import com.trade.exception.BusinessException;
import com.trade.repository.SupplierRepository;
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
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public Page<Supplier> getSuppliers(String keyword, String status, Pageable pageable) {
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("name"), kw),
                        cb.like(root.get("supplierCode"), kw),
                        cb.like(root.get("contact"), kw)
                ));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(root.get("status"), Supplier.SupplierStatus.valueOf(status.trim().toUpperCase())));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return supplierRepository.findAll(spec, pageable);
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException("供应商不存在"));
    }

    @Transactional
    public Supplier create(SupplierDTO dto) {
        if (supplierRepository.existsBySupplierCode(dto.getSupplierCode().trim())) {
            throw new BusinessException("供应商编码已存在");
        }
        Supplier s = new Supplier();
        fillFromDto(s, dto, true);
        return supplierRepository.save(s);
    }

    @Transactional
    public Supplier update(Long id, SupplierDTO dto) {
        Supplier s = getById(id);
        if (!s.getSupplierCode().equals(dto.getSupplierCode().trim())
                && supplierRepository.existsBySupplierCode(dto.getSupplierCode().trim())) {
            throw new BusinessException("供应商编码已存在");
        }
        fillFromDto(s, dto, false);
        return supplierRepository.save(s);
    }

    @Transactional
    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new BusinessException("供应商不存在");
        }
        supplierRepository.deleteById(id);
    }

    private void fillFromDto(Supplier s, SupplierDTO dto, boolean creating) {
        s.setSupplierCode(dto.getSupplierCode().trim());
        s.setName(dto.getName().trim());
        s.setContact(emptyToNull(dto.getContact()));
        s.setPhone(emptyToNull(dto.getPhone()));
        s.setEmail(emptyToNull(dto.getEmail()));
        s.setAddress(emptyToNull(dto.getAddress()));
        s.setTaxNumber(emptyToNull(dto.getTaxNumber()));
        s.setBankName(emptyToNull(dto.getBankName()));
        s.setBankAccount(emptyToNull(dto.getBankAccount()));
        s.setCreditRating(dto.getCreditRating());
        s.setDeliveryOnTimeRate(dto.getDeliveryOnTimeRate());
        s.setQualityPassRate(dto.getQualityPassRate());
        s.setRemark(emptyToNull(dto.getRemark()));
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            s.setStatus(Supplier.SupplierStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        } else if (creating) {
            s.setStatus(Supplier.SupplierStatus.ACTIVE);
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
