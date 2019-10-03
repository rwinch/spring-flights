package io.spring.sample.flighttracker.profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class UserProfileController {

	private final UserProfileRepository repository;

	public UserProfileController(UserProfileRepository repository) {
		this.repository = repository;
	}

	@MessageMapping("fetch.profile.me")
	public Mono<UserProfile> fetchProfile(@AuthenticationPrincipal Jwt jwt) {
		String login = jwt.getClaim("preferred_username");
		return this.repository.findByLogin(login);
	}

	@MessageMapping("fetch.profile.{login}")
	public Mono<PublicUserProfile> fetchPublicProfile(@DestinationVariable String login) {
		return this.repository.findByLogin(login).map(PublicUserProfile::new);
	}
}
