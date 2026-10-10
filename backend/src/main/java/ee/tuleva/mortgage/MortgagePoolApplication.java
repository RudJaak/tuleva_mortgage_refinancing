package ee.tuleva.mortgage;

import ee.tuleva.mortgage.auth.EidDirectoryTls;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MortgagePoolApplication {

  public static void main(String[] args) {
    EidDirectoryTls.allowRsaKeyExchange();
    SpringApplication.run(MortgagePoolApplication.class, args);
  }
}
