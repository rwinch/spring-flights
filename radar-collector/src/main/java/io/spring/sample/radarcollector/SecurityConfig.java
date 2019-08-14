package io.spring.sample.radarcollector;

import io.rsocket.SocketAcceptor;
import org.springframework.boot.rsocket.server.RSocketServerBootstrap;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.rsocket.interceptor.PayloadSocketAcceptorInterceptor;

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
	PayloadSocketAcceptorInterceptor rsocketInterceptor(RSocketSecurity rsocket) {
		rsocket
			.authorizePayload(authorize -> {
				authorize
					.route("listen.radar.*").authenticated()
					.anyExchange().permitAll();
			});
		return rsocket.build();
	}


	@Bean
	RSocketServerBootstrap rSocketServerBootstrap(
			RSocketServerFactory rSocketServerFactory,
			RSocketMessageHandler rSocketMessageHandler,
			PayloadSocketAcceptorInterceptor rsocketInterceptor) {
		SocketAcceptor delegate = rSocketMessageHandler.serverResponder();
		SocketAcceptor securityAcceptor = rsocketInterceptor.apply(delegate);
		return new RSocketServerBootstrap(rSocketServerFactory,
				securityAcceptor);
	}

//	@Bean
	// FIXME: Does not work because NettyRSocketServerFactory:124 sets acceptor after ServerRSocketFactoryCustomizer are applied
	ServerRSocketFactoryCustomizer securityCustomizer(PayloadSocketAcceptorInterceptor security,
			RSocketMessageHandler rSocketMessageHandler) {
		return serverRSocketFactory -> {
			SocketAcceptor delegate = rSocketMessageHandler.serverResponder();
			SocketAcceptor securityAcceptor = security.apply(delegate);
			serverRSocketFactory.acceptor(securityAcceptor);
			return serverRSocketFactory;
		};
	}
}
