package com.microservices.login.repository;

import com.microservices.login.entity.JwtToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JwtRepository extends JpaRepository<JwtToken, Long> {
    Optional<JwtToken> findByToken(String token);

    List<JwtToken> findByUsernameAndRevokedFalse(String username);

    @Modifying
    @Query("UPDATE JwtToken j SET j.revoked = true WHERE j.username = :username AND j.revoked = false")
    int revokeAllUserTokens(@Param("username") String username);

    @Modifying
    @Query("UPDATE JwtToken j SET j.revoked = true WHERE j.token = :token")
    int revokeToken(@Param("token") String token);
}
