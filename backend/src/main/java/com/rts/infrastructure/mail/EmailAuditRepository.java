package com.rts.infrastructure.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailAuditRepository extends JpaRepository<EmailAudit, String> {
}
