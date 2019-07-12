package io.spring.sample.radarcollector;

import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.plugins.RSocketInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.annotation.support.MetadataExtractor;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;
import rsocket.interceptor.PayloadChain;
import rsocket.interceptor.PayloadInterceptor;
import rsocket.interceptor.PayloadRSocketInterceptor;
import security.AuthenticationPayloadInterceptor;
import security.AuthorizationPayloadInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

@SpringBootApplication
public class RadarCollectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(RadarCollectorApplication.class, args);
	}

	@Bean
	ServerRSocketFactoryCustomizer securityCustomizer(RSocketMessageHandler handler) {
		return new ServerRSocketFactoryCustomizer() {

			@Override
			public RSocketFactory.ServerRSocketFactory apply(
					RSocketFactory.ServerRSocketFactory serverRSocketFactory) {
				return serverRSocketFactory.addResponderPlugin(security(handler.getMetadataExtractor()));
			}
		};
	}

	private static RSocketInterceptor security(MetadataExtractor extractor) {
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
		List<PayloadInterceptor> payloadInterceptors = Arrays
				.asList(new LoggingPayloadInterceptor(extractor), new AuthenticationPayloadInterceptor(manager), new AuthorizationPayloadInterceptor(hasRole("ADMIN")));

		return new PayloadRSocketInterceptor(payloadInterceptors);
	}

	static class LoggingPayloadInterceptor implements PayloadInterceptor {
		private final MetadataExtractor extractor;

		LoggingPayloadInterceptor(MetadataExtractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public Mono<Void> intercept(Payload payload, PayloadChain chain) {
			Map<String, Object> headers = this.extractor
					.extract(payload, new MimeType("message", "x.rsocket.routing.v0"));
			System.out.println(headers.get(MetadataExtractor.ROUTE_KEY));
			return chain.next(payload);
		}
	}
}
