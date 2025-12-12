package com.example.accounts;

import com.example.accounts.dto.AccountsContactInfoDto;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(value = {AccountsContactInfoDto.class})
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "FinBankX Accounts microservice REST API Documentation",
                description = "Secure Banking Platform - Accounts Microservice REST API Documentation",
                version = "v1",
                contact = @Contact(
                        name = "FinBankX Development Team",
                        email = "dev@finbankx.com",
                        url = "https://www.finbankx.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        ),
        externalDocs = @ExternalDocumentation(
                description =  "FinBankX Accounts microservice REST API Documentation",
                url = "https://www.finbankx.com/swagger-ui.html"
        )
)
public class AccountsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsApplication.class, args);
    }

}
