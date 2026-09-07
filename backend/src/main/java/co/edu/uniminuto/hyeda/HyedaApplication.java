package co.edu.uniminuto.hyeda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HyedaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HyedaApplication.class, args);
    }
}
