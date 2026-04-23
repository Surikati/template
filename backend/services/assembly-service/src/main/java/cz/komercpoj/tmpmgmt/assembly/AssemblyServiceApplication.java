package cz.komercpoj.tmpmgmt.assembly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AssemblyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssemblyServiceApplication.class, args);
    }
}
