package edusecurity.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad estándar (stateless + JWT, sin CORS) reutilizable por
 * todos los microservicios servlet. Se activa con {@code app.security.enabled=true}
 * y las rutas públicas se declaran en {@code app.security.permit-all}.
 *
 * <p>Se declara <b>antes</b> que la configuración por defecto de Spring Boot y
 * <b>antes</b> que {@code ManagementWebSecurityAutoConfiguration} (Actuator),
 * para que ambas cedan ante nuestra {@code SecurityFilterChain}. Sin ese
 * orden, la cadena {@code managementSecurityFilterChain} de Actuator se
 * registraría primero y la nuestra se anularía por
 * {@code @ConditionalOnMissingBean}, dejando CSRF habilitado y Basic auth
 * como entry point.</p>
 */
@AutoConfiguration(
        before = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        beforeName = "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({JwtProperties.class, ApiSecurityProperties.class})
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtSupport jwtSupport(JwtProperties jwtProperties) {
        return new JwtSupport(jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiAuthenticationFilter apiAuthenticationFilter(JwtSupport jwtSupport,
                                                          ApiSecurityProperties properties,
                                                          ObjectMapper objectMapper) {
        return new ApiAuthenticationFilter(jwtSupport, properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiAuthenticationFilter filter,
            ApiSecurityProperties properties,
            JwtAuthenticationEntryPoint entryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS solo en el gateway (0.7): los servicios no son accesibles desde el navegador
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    properties.getPermitAll()
                            .forEach(path -> auth.requestMatchers(path).permitAll());
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(name = "authenticationManager")
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
