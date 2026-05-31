package upc.com.pe.backendplannia.iam.infrastructure.authorization.sfs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import upc.com.pe.backendplannia.iam.domain.model.aggregates.User;

import java.util.Collection;
import java.util.Collections;

/**
 * This class is responsible for providing the user details to the Spring Security framework.
 * It implements the UserDetails interface.
 */

@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {

    private final String email;
    @JsonIgnore
    private final String password;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * This constructor initializes the UserDetailsImpl object.
     * @param email The email.
     * @param password The password.
     */
    public UserDetailsImpl(String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }


    /**
     * This method is responsible for building the UserDetailsImpl object from the User object.
     * @param user The user object.
     * @return The UserDetailsImpl object.
     */
    public static UserDetailsImpl build(User user) {
        // As we don't need roles, return an empty list of authorities
        Collection<GrantedAuthority> authorities = Collections.emptyList();  // No authorities are assigned
        return new UserDetailsImpl(
                user.getEmail(), // Use email as the identifier
                user.getPassword(),
                authorities);
    }

    /**
     * This method is required by UserDetails interface.
     * @return The username, which in this case is the email.
     */
    @Override
    public String getUsername() {
        return email; // Return email as username
    }

    // Optionally override other methods as needed, but they're already set to "true" by default.
}

