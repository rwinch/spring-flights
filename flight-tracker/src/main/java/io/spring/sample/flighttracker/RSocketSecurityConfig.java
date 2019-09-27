package io.spring.sample.flighttracker;

import io.spring.sample.flighttracker.profile.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.rsocket.PayloadInterceptorOrder;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.rsocket.api.PayloadExchange;
import org.springframework.security.rsocket.authentication.AuthenticationPayloadInterceptor;
import org.springframework.security.rsocket.authentication.PayloadExchangeAuthenticationConverter;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.security.rsocket.metadata.BearerTokenMetadata;
import org.springframework.security.rsocket.util.matcher.PayloadExchangeAuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author Rob Winch
 */
@Configuration
@EnableReactiveMethodSecurity
public class RSocketSecurityConfig {
	@Autowired
	Friends friends;

	@Bean
	PayloadSocketAcceptorInterceptor rsocketSecurity(RSocketSecurity rsocket, AuthenticationPayloadInterceptor jwt) {
		rsocket
			.authorizePayload(authz ->
				authz
					.route("fetch.profile.me").authenticated()
//					.route("fetch.profile.{username}").access((a,c) -> checkFriends(a, c))
					.anyRequest().authenticated()
					.anyExchange().permitAll()
			)
			.addPayloadInterceptor(jwt);
		return rsocket.build();
	}

	private Mono<AuthorizationDecision> checkFriends(Mono<Authentication> a,
			PayloadExchangeAuthorizationContext c) {
		return a.map(Authentication::getPrincipal)
			.cast(UserProfile.class)
			.map(UserProfile::getLogin)
			.map(currentLogin -> friends.isLoginFriendOf(currentLogin, (String) c.getVariables().get("username")))
			.map(AuthorizationDecision::new);
	}

	@Bean
	AuthenticationPayloadInterceptor jwt(ReactiveJwtDecoder decoder, MetadataExtractorBearerTokenConverter bearerTokenConverter,
			UserProfileAuthenticationConverter converter) {
		JwtReactiveAuthenticationManager manager = new JwtReactiveAuthenticationManager(decoder);
		manager.setJwtAuthenticationConverter(converter);
		AuthenticationPayloadInterceptor result = new AuthenticationPayloadInterceptor(manager);
		result.setAuthenticationConverter(bearerTokenConverter);
		result.setOrder(PayloadInterceptorOrder.JWT_AUTHENTICATION.getOrder());
		return result;
	}

	@Component
	class MetadataExtractorBearerTokenConverter implements
			PayloadExchangeAuthenticationConverter {

		private final MetadataExtractor metadataExtractor;

		MetadataExtractorBearerTokenConverter(RSocketMessageHandler handler) {
			this.metadataExtractor = handler.getMetadataExtractor();
		}

		@Override
		public Mono<Authentication> convert(PayloadExchange exchange) {
			Map<String, Object> data = this.metadataExtractor
					.extract(exchange.getPayload(), exchange.getMetadataMimeType());
			return Mono.justOrEmpty(data.get(BearerTokenMetadata.BEARER_AUTHENTICATION_MIME_TYPE.toString()))
					.cast(String.class)
					.map(BearerTokenAuthenticationToken::new);
		}
	}
}
