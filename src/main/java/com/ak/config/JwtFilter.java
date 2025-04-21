package com.ak.config;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ak.service.MyUserDetailsService;
import com.ak.utils.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private JwtUtils jwtUtils;

	ApplicationContext context;

	public JwtFilter(JwtUtils jwtUtils, ApplicationContext context) {
		super();
		this.jwtUtils = jwtUtils;
		this.context = context;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		String token = null;
		String username = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
			try {
				username = jwtUtils.extractUsername(token, jwtUtils.getJwtSecret());
			} catch (ExpiredJwtException e) {
				System.out.println("JWT token expired from Authorization header");
			} catch (Exception e) {
				System.out.println("JWT token error: " + e.getMessage());
			}
		} else {
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if ("jwtToken".equals(cookie.getName())) {
						token = cookie.getValue().trim(); // Remove any accidental whitespace
						try {
							username = jwtUtils.extractUsername(token, jwtUtils.getJwtSecret());
						} catch (ExpiredJwtException e) {
							System.out.println("JWT token expired from cookie");
						} catch (Exception e) {
							System.out.println("JWT token error in cookie: " + e.getMessage());
						}
						break;
					}
				}
			}
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = context.getBean(MyUserDetailsService.class).loadUserByUsername(username);
			if (jwtUtils.validateToken(token, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);

			}
		}

		filterChain.doFilter(request, response);
	}

}