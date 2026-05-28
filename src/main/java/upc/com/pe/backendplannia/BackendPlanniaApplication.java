package upc.com.pe.backendplannia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BackendPlanniaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendPlanniaApplication.class, args);
    }

}
