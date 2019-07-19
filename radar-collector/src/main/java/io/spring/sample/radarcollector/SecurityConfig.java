package io.spring.sample.radarcollector;

import io.rsocket.Payload;
import io.rsocket.plugins.RSocketInterceptor;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.annotation.support.MetadataExtractor;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.rsocket.interceptor.*;
import org.springframework.security.rsocket.interceptor.authentication.*;
import org.springframework.security.rsocket.interceptor.authorization.AuthorizationPayloadInterceptor;
import org.springframework.security.rsocket.interceptor.authorization.PayloadMatcherReactiveAuthorizationManager;
import org.springframework.security.rsocket.metadata.SecurityMetadataFlyweight;
import org.springframework.security.rsocket.util.PayloadMatcherEntry;
import org.springframework.security.rsocket.util.RoutePayloadExchangeMatcher;
import org.springframework.util.RouteMatcher;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager.authenticated;

/**
 * @author Rob Winch
 */
@Configuration
public class SecurityConfig {


	@Bean
	ServerRSocketFactoryCustomizer securityCustomizer(RSocketMessageHandler handler) {
		return serverRSocketFactory -> serverRSocketFactory.addResponderPlugin(security(handler));
	}

	private static RSocketInterceptor security(RSocketMessageHandler handler) {
		MetadataExtractor extractor = handler.getMetadataExtractor();
		RouteMatcher routeMatcher = handler.getRouteMatcher();
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
		MapReactiveUserDetailsService uds = new MapReactiveUserDetailsService(
				rob, rossen);
		ReactiveAuthenticationManager manager = new UserDetailsRepositoryReactiveAuthenticationManager(uds);
		PayloadMatcherReactiveAuthorizationManager authorization =
				PayloadMatcherReactiveAuthorizationManager.builder()
						.add(new PayloadMatcherEntry<>(new RoutePayloadExchangeMatcher(extractor, routeMatcher, "listen.radar.*"), authenticated()))
						.add(new PayloadMatcherEntry<>(p -> RoutePayloadExchangeMatcher.MatchResult.match(), (a,ctx) -> Mono
								.just(new AuthorizationDecision(true))))
						.build();
		List<PayloadInterceptor> payloadInterceptors =
				Arrays.asList(new LoggingPayloadInterceptor(extractor),
						new AuthenticationPayloadInterceptor(manager),
						new AnonymousPayloadInterceptor("anonymousUser"),
						new AuthorizationPayloadInterceptor(authorization));

		return new PayloadRSocketInterceptor(payloadInterceptors);
	}

	static class LoggingPayloadInterceptor implements PayloadInterceptor {
		private final MetadataExtractor extractor;

		LoggingPayloadInterceptor(MetadataExtractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public Mono<Void> intercept(PayloadExchange exchange, PayloadInterceptorChain chain) {
			Payload payload = exchange.getPayload();
			Map<String, Object> headers = this.extractor
					.extract(payload, exchange.getMetadataMimeType());
			Object route = headers.get(MetadataExtractor.ROUTE_KEY);
			SecurityMetadataFlyweight.UsernamePassword credentials = SecurityMetadataFlyweight
					.readBasic(payload.metadata()).orElse(null);
			//			System.out.println(headers + " " + credentials);
			return chain.next(exchange)
					.onErrorMap(AccessDeniedException.class, e -> {
						System.out.println(route + " was denied for user " + credentials + " and route " + route);
						return e;
					});
		}
	}
}
