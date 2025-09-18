package com.neo.springapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.neo.springapp.model.TransferRecord;

@Repository
public interface TransferRepository extends JpaRepository<TransferRecord, Long> {
}
