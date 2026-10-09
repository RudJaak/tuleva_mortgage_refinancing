package ee.tuleva.mortgage;

import org.springframework.boot.SpringApplication;

public class TestMortgagePoolApplication {

  public static void main(String[] args) {
    SpringApplication.from(MortgagePoolApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
