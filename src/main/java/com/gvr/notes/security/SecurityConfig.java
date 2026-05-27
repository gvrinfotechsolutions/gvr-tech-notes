package com.gvr.notes.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http

				// AUTHORIZATION CONFIGURATION
				.authorizeHttpRequests(auth -> auth

						// PUBLIC PAGES
						.requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()

						// ADMIN ONLY URLS
						.requestMatchers("/saveTopic", "/edit/**", "/delete/**", "/admin/**").hasRole("ADMIN")

						// USER + ADMIN ACCESS
						.requestMatchers("/", "/topics/**", "/view-topic/**").hasAnyRole("ADMIN", "USER")

						// ALL OTHER REQUESTS
						.anyRequest().authenticated())

				// LOGIN CONFIGURATION
				.formLogin(form -> form

						.loginPage("/login")

						.defaultSuccessUrl("/", true)

						.failureUrl("/login?error=true")

						.permitAll())

				// LOGOUT CONFIGURATION
				.logout(logout -> logout

						.logoutUrl("/logout")

						.logoutSuccessUrl("/login?logout=true")

						.invalidateHttpSession(true)

						.clearAuthentication(true)

						.permitAll())

				// CSRF CONFIGURATION
				.csrf(csrf -> csrf.disable());

		return http.build();
	}
}