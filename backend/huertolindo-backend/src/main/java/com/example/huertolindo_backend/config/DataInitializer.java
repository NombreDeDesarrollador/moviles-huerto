package com.example.huertolindo_backend.config;

import com.example.huertolindo_backend.model.Rol;
import com.example.huertolindo_backend.model.Sector;
import com.example.huertolindo_backend.model.Usuario;
import com.example.huertolindo_backend.repository.SectorRepository;
import com.example.huertolindo_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final SectorRepository sectores;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UsuarioRepository usuarios, SectorRepository sectores, PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.usuarios = usuarios;
        this.sectores = sectores;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setCorreo(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRol(Rol.ADMIN);
            admin.setActivo(true);
            usuarios.save(admin);
        }
        if (sectores.count() == 0) {
            Sector sector1 = new Sector();
            sector1.setNombre("Huerto principal");
            sector1.setDescripcion("Sector de prueba del huerto escolar");
            sector1.setUbicacion("Patio central");
            sectores.save(sector1);

            Sector sector2 = new Sector();
            sector2.setNombre("Invernadero");
            sector2.setDescripcion("Cultivos protegidos");
            sector2.setUbicacion("Patio norte");
            sectores.save(sector2);
        }
    }
}
