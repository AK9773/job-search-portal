package com.ak.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration.ms}")
	private int jwtExpirationMs;

	@Value("${refresh.jwt.secret}")
	private String refreshSecret;

	@Value("${refresh.jwt.expiration.ms}")
	private int refreshExpirationMs;

	public String getJwtSecret() {
		return jwtSecret;
	}

	public String getRefreshSecret() {
		return refreshSecret;
	}

	public String generateJwtToken(String username) {
		return generateToken(username, jwtSecret, jwtExpirationMs);
	}

	public String generateRefreshToken(String username) {
		return generateToken(username, refreshSecret, refreshExpirationMs);

	}

	public String generateToken(String username, String secret, int expirationMs) {
		Map<String, Object> claims = new HashMap<>();
		return Jwts.builder().claims().add(claims).subject(username).issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expirationMs)).and().signWith(getKey(secret))
				.compact();
	}

	public boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token, jwtSecret);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token, jwtSecret);
	}

	public boolean validateRefreshToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token, refreshSecret);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token, refreshSecret);
	}

	public String extractUsername(String token, String secret) {
		return extractClaim(token, Claims::getSubject, secret);
	}

	public Date extractExpiration(String token, String secret) {
		return extractClaim(token, Claims::getExpiration, secret);
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, String secret) {
		final Claims claims = extractAllClaims(token, secret);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token, String secret) {
		SecretKey key = getKey(secret);
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	private Boolean isTokenExpired(String token, String secret) {
		return extractExpiration(token, secret).before(new Date());
	}

	public SecretKey getKey(String secret) {
		SecretKey hmacShaKeyFor = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		return hmacShaKeyFor;
	}
}