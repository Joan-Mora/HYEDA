package co.edu.uniminuto.hyeda;

import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(HyedaProperties.class)
@EnableScheduling
public class HyedaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HyedaApplication.class, args);
    }
}
