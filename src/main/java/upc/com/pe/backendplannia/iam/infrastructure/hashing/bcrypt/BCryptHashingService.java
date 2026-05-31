package upc.com.pe.backendplannia.iam.infrastructure.hashing.bcrypt;

import org.springframework.security.crypto.password.PasswordEncoder;
import upc.com.pe.backendplannia.iam.application.internal.outboundedservices.HashingService;

public interface BCryptHashingService extends HashingService, PasswordEncoder {
    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
