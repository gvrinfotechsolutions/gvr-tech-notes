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
						.requestMatchers("/login", "/register", "/error", "/css/**", "/js/**", "/images/**").permitAll()

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

						.failureHandler((request, response, exception) -> {
							String reason = (exception instanceof org.springframework.security.authentication.DisabledException)
									? "pending" : "true";
							response.sendRedirect("/login?error=" + reason);
						})

						.permitAll())

				// LOGOUT CONFIGURATION
				.logout(logout -> logout

						.logoutUrl("/logout")

						.logoutSuccessUrl("/login?logout=true")

						.invalidateHttpSession(true)

						.clearAuthentication(true)

						.permitAll())

				// CSRF is enabled; JS fetch calls must include the X-CSRF-TOKEN header
				.csrf(csrf -> csrf
						.ignoringRequestMatchers("/trackView/**"));

		return http.build();
	}
}