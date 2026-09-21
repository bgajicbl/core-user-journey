package com.myedspace.cuj.repository;

import com.myedspace.cuj.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Query("select p from Purchase p join fetch p.course where p.invitationToken = :invitationToken")
    Optional<Purchase> findByInvitationToken(String invitationToken);
}
