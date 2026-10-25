package leave_magament.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService users(@Value("${app.employee-password}") String employeePassword,
                             @Value("${app.manager-password}") String managerPassword) {
        if (employeePassword.isBlank() || managerPassword.isBlank()
                || employeePassword.length() < 12 || managerPassword.length() < 12
                || employeePassword.equals(managerPassword)) {
            throw new IllegalArgumentException("Configure dos claves distintas de al menos 12 caracteres");
        }
        var encoder = new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(
            User.withUsername("employee").password(encoder.encode(employeePassword)).roles("EMPLOYEE").build(),
            User.withUsername("manager").password(encoder.encode(managerPassword)).roles("MANAGER").build());
    }
    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            // API sin cookies ni sesiones: exigir Authorization en cada peticion.
            // No utilizar este esquema como login de navegador; documentado para clientes API.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").denyAll()
                .requestMatchers("/api/leaves/*/approve", "/api/leaves/*/reject").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/leaves").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.PUT, "/api/leaves/*").hasRole("EMPLOYEE")
                .requestMatchers("/api/**").hasAnyRole("EMPLOYEE", "MANAGER")
                .anyRequest().denyAll())
            .addFilterBefore(new ApiWriteHeaderFilter(), org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(401);
                res.setHeader("WWW-Authenticate", "Basic realm=\"leave-api\"");
                res.setContentType("application/problem+json");
                res.getWriter().write("{\"title\":\"No autenticado\",\"status\":401}");
            }))
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> {
                res.setStatus(403); res.setContentType("application/problem+json");
                res.getWriter().write("{\"title\":\"Acceso denegado\",\"status\":403}");
            })).build();
    }
}
