package com.company.ra.repository;

import com.company.ra.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by username
     *
     * @param username Username
     * @param pageable Pagination info
     * @return List of AuditLogs
     */
    List<AuditLog> findByUsername(String username, Pageable pageable);

    /**
     * Find audit logs by action
     *
     * @param action Action name
     * @param pageable Pagination info
     * @return List of AuditLogs
     */
    List<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Find audit logs by resource type and resource ID
     *
     * @param resourceType Resource type
     * @param resourceId Resource ID
     * @return List of AuditLogs
     */
    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    /**
     * Find audit logs within time range
     *
     * @param start Start timestamp
     * @param end End timestamp
     * @param pageable Pagination info
     * @return List of AuditLogs
     */
    List<AuditLog> findByTimestampBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Find audit logs by username and action
     *
     * @param username Username
     * @param action Action name
     * @param pageable Pagination info
     * @return List of AuditLogs
     */
    List<AuditLog> findByUsernameAndAction(String username, String action, Pageable pageable);
}
