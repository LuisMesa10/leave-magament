package leave_magament.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

// Un encabezado no simple evita escrituras CSRF con credenciales Basic recordadas
// por un navegador. No se habilita CORS; este backend se consume desde clientes API.
public final class ApiWriteHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())
                && !"leave-api".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(403);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Se requiere X-Requested-With: leave-api\",\"status\":403}");
            return;
        }
        chain.doFilter(request, response);
    }
}
