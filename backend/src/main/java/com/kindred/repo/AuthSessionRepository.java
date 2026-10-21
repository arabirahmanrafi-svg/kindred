package com.kindred.repo;

import com.kindred.domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    /** Loads the session and its account in one query. */
    @Query("select s from AuthSession s join fetch s.account where s.tokenHash = :hash")
    Optional<AuthSession> findWithAccount(@Param("hash") String tokenHash);

    @Modifying
    @Query("delete from AuthSession s where s.tokenHash = :hash")
    int deleteByHash(@Param("hash") String tokenHash);

    @Modifying
    @Query("delete from AuthSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
