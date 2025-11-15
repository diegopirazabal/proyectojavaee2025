package hcen.central.inus.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import java.util.List;

/**
 * JAX-RS filter that propagates the JWT subject/roles stored by {@link JWTAuthenticationFilter}
 * into the JAX-RS {@link SecurityContext}. This allows resources to inject the security context
 * and obtain the authenticated user identity without re-validating the token.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTSecurityContextRequestFilter implements ContainerRequestFilter {

    private static final String CLAIMS_ATTRIBUTE = "jwtClaims";

    @Context
    private HttpServletRequest httpServletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Object claimsAttr = httpServletRequest.getAttribute(CLAIMS_ATTRIBUTE);
        if (!(claimsAttr instanceof Claims claims)) {
            return; // No JWT claims available for this request
        }

        SecurityContext currentContext = requestContext.getSecurityContext();
        Principal principal = () -> claims.getSubject();

        requestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String role) {
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                return roles != null && roles.contains(role);
            }

            @Override
            public boolean isSecure() {
                return currentContext != null && currentContext.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return "JWT";
            }
        });
    }
}
