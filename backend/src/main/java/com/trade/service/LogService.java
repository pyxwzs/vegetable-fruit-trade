package com.trade.service;

import com.trade.entity.OperationLog;
import com.trade.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {
    private final OperationLogRepository operationLogRepository;

    public void saveLog(OperationLog log) {
        operationLogRepository.save(log);
    }

    public Page<OperationLog> getLogs(String username, String module,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      Boolean success, Pageable pageable) {
        return operationLogRepository.findAll((root, query, cb) -> {
            var predicates = cb.conjunction();

            if (username != null && !username.isEmpty()) {
                predicates = cb.and(predicates, cb.like(root.get("username"), "%" + username + "%"));
            }

            if (module != null && !module.isEmpty()) {
                predicates = cb.and(predicates, cb.like(root.get("module"), "%" + module + "%"));
            }

            if (startTime != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }

            if (endTime != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createTime"), endTime));
            }

            if (success != null) {
                predicates = cb.and(predicates, cb.equal(root.get("success"), success));
            }

            return predicates;
        }, pageable);
    }

    public void cleanOldLogs(int days) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
        operationLogRepository.deleteByCreateTimeBefore(beforeDate);
    }
}