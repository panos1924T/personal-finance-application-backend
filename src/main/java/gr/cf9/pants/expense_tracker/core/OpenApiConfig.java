package gr.cf9.pants.expense_tracker.core;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/*
  The practical effect is that Swagger UI shows an "Authorize" button where you paste your JWT token.
 */
@SecurityScheme(
        name = "Bearer Authentication",     // Must match @SecurityRequirement's name
        type = SecuritySchemeType.HTTP,     // Authentication is done via HTTP header
        bearerFormat = "JWT",               // Informational, tells Swagger UI the token format is JWT
        scheme = "bearer"                   // The HTTP scheme is Bearer, meaning Authorization: Bearer <token>
)
public class OpenApiConfig {

    /*
        Provides the metadata that appears in Swagger UI's header section —
        purely informational, no functional impact on the API itself
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ET API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing each individual's personal finance.
                                Provides endpoints for managing users, accounts, transactions and categories.
                                Authentication is done via JWT Bearer tokens.
                                Obtain a token from /api/auth/authenticate before using secured endpoints.
                        """)
                        .contact(new Contact()
                                .name("PanTs")
                                .email("tsitsikaspan@gmail.com")
                                .url("https://panos1924t.github.io/personal-cv/"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }

    /*
        Automatically injects 401 response into every secured operation's
        Swagger documentation, so we don't repeat @ApiResponse annotations everywhere.
    */
    @Bean
    public OperationCustomizer globalSecurityResponses() {
        return (operation, handlerMethod) -> {
            boolean isSecured = handlerMethod.hasMethodAnnotation(SecurityRequirement.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(SecurityRequirement.class);

            if (isSecured) {
                operation.getResponses()
                        .addApiResponse("401", new ApiResponse().description("Unauthorized - JWT token is missing or invalid"));
            }
            return operation;
        };
    }
}