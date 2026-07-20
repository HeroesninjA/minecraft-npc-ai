package ro.ainpc.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AinpcMcpServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AinpcMcpServiceApplication.class, args);
    }

    @Bean
    public Gson gson() {
        return new GsonBuilder().create();
    }
}
