package org.example.agent.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class Knife4jConfig {

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            operation.addParametersItem(
                    new HeaderParameter()
                            .name("satoken")
                            .description("Sa-Token 认证令牌")
                            .required(false)
            );
            return operation;
        };
    }
}
