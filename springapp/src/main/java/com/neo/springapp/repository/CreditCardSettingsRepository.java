package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardSettingsRepository extends JpaRepository<CreditCardSettings, Long> {
}
