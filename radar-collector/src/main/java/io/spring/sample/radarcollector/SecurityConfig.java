package io.spring.sample.radarcollector;

import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.rsocket.interceptor.PayloadRSocketInterceptor;

/**
 * @author Rob Winch
 */
@Configuration
@EnableRSocketSecurity
public class SecurityConfig {

	@Bean
	MapReactiveUserDetailsService uds() {
		UserDetails rob = User.withDefaultPasswordEncoder()
				.username("rob")
				.password("password")
				.roles("USER", "ADMIN")
				.build();
		UserDetails rossen = User.withDefaultPasswordEncoder()
				.username("rossen")
				.password("password")
				.roles("USER")
				.build();
		return new MapReactiveUserDetailsService(rob, rossen);
	}

	@Bean
	PayloadRSocketInterceptor rsocketInterceptor(RSocketSecurity rsocket) {
		rsocket
			.authorizePayload(authorize -> {
				authorize
					.route("listen.radar.*").authenticated()
					.anyExchange().permitAll();
			});
		return rsocket.build();
	}

	@Bean
	ServerRSocketFactoryCustomizer securityCustomizer(PayloadRSocketInterceptor rsocketInterceptor) {
		return serverRSocketFactory -> serverRSocketFactory.addResponderPlugin(rsocketInterceptor);
	}
}
