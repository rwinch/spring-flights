package io.spring.sample.flighttracker;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Rob Winch
 */
@Component
public class Friends {
	// Brian has added Rossen as a friend
	private Map<String, Set<String>> loginToFriendLogins =
			Collections.singletonMap("brian", Collections.singleton("rossen"));

	public boolean isLoginFriendOf(String currentUserLogin, String loginToTest) {
		return this.loginToFriendLogins.getOrDefault(currentUserLogin, Collections.emptySet())
				.contains(loginToTest);
	}
}
