package io.spring.sample.radarcollector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class RadarCollectorApplicationTests {

	@Test
	public void contextLoads() {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		assertThat(encoder.matches("password", "{bcrypt}$2a$10$ZmNeaerYvZBR5lHl7sp4mOWj1GuZUi9pbEqiD1YlV3b0goAnRolW2")).isTrue();
	}

}
