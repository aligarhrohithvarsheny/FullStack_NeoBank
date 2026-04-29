package com.neo.springapp.repository;

import com.neo.springapp.model.PgPaymentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgPaymentLinkRepository extends JpaRepository<PgPaymentLink, Long> {

    Optional<PgPaymentLink> findByLinkToken(String linkToken);

    Optional<PgPaymentLink> findByLinkId(String linkId);

    List<PgPaymentLink> findByRecipientUpiIdOrderByCreatedAtDesc(String recipientUpiId);

    List<PgPaymentLink> findByMerchantIdOrderByCreatedAtDesc(String merchantId);

    List<PgPaymentLink> findByRecipientUpiIdAndStatusOrderByCreatedAtDesc(String recipientUpiId, String status);
}
