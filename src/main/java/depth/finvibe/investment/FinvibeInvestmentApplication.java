package depth.finvibe.investment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@ConfigurationPropertiesScan
public class FinvibeInvestmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinvibeInvestmentApplication.class, args);
    }

}
