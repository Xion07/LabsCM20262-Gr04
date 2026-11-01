package co.edu.udea.motoscaribe.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AutogestionApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutogestionApiApplication.class, args);
	}

}
