package com.neo.springapp.repository;

import com.neo.springapp.model.JointTransferApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JointTransferApprovalRepository extends JpaRepository<JointTransferApproval, Long> {
    List<JointTransferApproval> findByApproverUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<JointTransferApproval> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
