package edusecurity.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import edusecurity.gateway.security.JwtValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GatewayApplicationTests {

    @Autowired
    private JwtValidator jwtValidator;

    @Test
    void contextLoads() {
        assertThat(jwtValidator).isNotNull();
    }
}
