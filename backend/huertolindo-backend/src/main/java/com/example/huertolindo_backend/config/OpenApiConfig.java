package com.example.huertolindo_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI huertoLindoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HuertoLindo API")
                        .description("API REST para el sistema de gestión del huerto escolar HuertoLindo. "
                                + "Proporciona operaciones CRUD completas para la administración de sectores, "
                                + "cultivos, tareas agrícolas, evidencias fotográficas, reportes de incidencias, "
                                + "validaciones docentes y usuarios del sistema.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo HuertoLindo")
                                .email("soporte@huertolindo.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor Local (Desarrollo)"),
                        new Server().url("http://10.0.2.2:8080").description("Emulador Android")
                ));
    }
}
