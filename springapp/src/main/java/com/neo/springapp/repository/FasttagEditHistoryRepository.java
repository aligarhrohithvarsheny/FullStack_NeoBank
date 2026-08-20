package com.neo.springapp.repository;

import com.neo.springapp.model.FasttagEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FasttagEditHistoryRepository extends JpaRepository<FasttagEditHistory, Long> {
    List<FasttagEditHistory> findByFasttagIdOrderByChangedAtDesc(Long fasttagId);
}
