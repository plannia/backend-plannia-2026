package upc.com.pe.backendplannia.iam.infrastructure.authorization.sfs.model;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

/**
 * This class is used to build the UsernamePasswordAuthenticationToken object
 * that is used to authenticate the user.
 */
public class EmailPasswordAuthenticationTokenBuilder {

    /**
     * This method is responsible for building the EmailPasswordAuthenticationToken object.
     * @param principal The user details.
     * @param request The HTTP request.
     * @return The EmailPasswordAuthenticationToken object.
     * @see UsernamePasswordAuthenticationToken
     * @see UserDetails
     */
    public static UsernamePasswordAuthenticationToken build(UserDetails principal, HttpServletRequest request) {
        var emailPasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        emailPasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return emailPasswordAuthenticationToken;
    }
}