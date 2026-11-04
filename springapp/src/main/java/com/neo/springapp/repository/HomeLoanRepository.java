package com.neo.springapp.repository;

import com.neo.springapp.model.HomeLoan;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeLoanRepository extends JpaRepository<HomeLoan, Long> {
    List<HomeLoan> findByAccountNumberOrderByApplicationDateDesc(String accountNumber);
    Optional<HomeLoan> findByApplicationId(String applicationId);
    Optional<HomeLoan> findByLoanAccountNumber(String loanAccountNumber);
    List<HomeLoan> findByStatus(String status);
    List<HomeLoan> findAllByOrderByApplicationDateDesc();

    @Query("""
            select h.fdReceiptPath as fdReceiptPath,
                   h.modelDocPath as modelDocPath,
                   h.signaturePath as signaturePath,
                   h.vehicleInvoicePath as vehicleInvoicePath,
                   h.vehicleImagePaths as vehicleImagePaths,
                   h.landRecordPath as landRecordPath,
                   h.agricultureEstimatePath as agricultureEstimatePath,
                   h.cropEstimatePath as cropEstimatePath
            from HomeLoan h where h.id = :id
            """)
    Optional<HomeLoanDocumentPaths> findDocumentPathsById(@Param("id") Long id);

    interface HomeLoanDocumentPaths {
        String getFdReceiptPath();
        String getModelDocPath();
        String getSignaturePath();
        String getVehicleInvoicePath();
        String getVehicleImagePaths();
        String getLandRecordPath();
        String getAgricultureEstimatePath();
        String getCropEstimatePath();
    }
}
