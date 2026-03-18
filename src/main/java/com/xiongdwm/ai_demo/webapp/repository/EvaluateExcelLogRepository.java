package com.xiongdwm.ai_demo.webapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.ai_demo.webapp.entities.EvaluateExcelLog;

public interface EvaluateExcelLogRepository extends JpaRepository<EvaluateExcelLog, Long> {
    List<EvaluateExcelLog> findBySnapshotId(Long snapshotId);
    List<EvaluateExcelLog> findBySnapshotIdIsNull();
}
