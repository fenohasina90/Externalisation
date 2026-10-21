package com.cabex.backend.security;

import com.cabex.backend.configuration.repository.ConfigurationRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final ConfigurationRepository configurationRepository;

    public JwtService(@Value("${app.jwt.secret}") String secretKey,
                      @Value("${app.jwt.expiration-ms:3600000}") long expirationMs,
                      ConfigurationRepository configurationRepository) {
        this.signingKey = construireCle(secretKey);
        this.expirationMs = expirationMs;
        this.configurationRepository = configurationRepository;
    }

    private static SecretKey construireCle(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET est obligatoire : fournissez une clé Base64 d'au moins 32 octets.");
        }
        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey.trim());
        } catch (JwtException e) {
            throw new IllegalStateException("JWT_SECRET doit être une chaîne Base64 valide.", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET doit contenir au moins 32 octets (256 bits) après décodage Base64.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof UserPrincipal up) {
            claims.put("role", up.getRoleName());
            claims.put("roleId", up.getRoleId());
            claims.put("userId", up.getId());
        }
        return buildToken(claims, userDetails.getUsername(), dureeExpirationMs());
    }

    /**
     * Durée de validité des jetons : valeur d'administration DUREE_SESSION_MINUTES
     * (appliquée aux prochaines connexions), avec repli sur la propriété
     * {@code app.jwt.expiration-ms}.
     */
    private long dureeExpirationMs() {
        try {
            int minutes = configurationRepository.findByCle("DUREE_SESSION_MINUTES")
                    .map(config -> Integer.parseInt(config.getValeur().trim()))
                    .orElse(0);
            if (minutes > 0) {
                return minutes * 60_000L;
            }
        } catch (NumberFormatException e) {
            // Valeur mal formée : repli sur la propriété
        }
        return expirationMs;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSignInKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        return signingKey;
    }
}
