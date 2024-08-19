package net.brubio.security;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class DatabaseWebSecurity /*extends WebSecurityConfigurerAdapter*/{
	
	/*@Autowired
	private DataSource dataSource;
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.jdbcAuthentication().dataSource(dataSource);
	}*/
	
	/*@Autowired
	private DataSource dataSource;*/
	
	@Bean	
	protected UserDetailsManager usersCustom(DataSource dataSource) {
		JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
		users.setUsersByUsernameQuery("select username, password, estatus from Usuarios where username=?");
		users.setAuthoritiesByUsernameQuery("select u.username, p.perfil from UsuarioPerfil up " + 
				"inner join Usuarios u on u.id = up.idUsuario " + 
				"inner join Perfiles p on p.id = up.idPerfil " + 
				"where u.username = ?");
		return users;
	}
	
	/*
	@Bean	
	protected void ConfigureUsers() {
		JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
		users.setUsersByUsernameQuery("select username, password, estatus from Usuarios where username=?");
		users.setCreateAuthoritySql("select u.username, p.perfil from UsuarioPerfil up " + 
				"inner join Usuarios u on u.id = up.idUsuario " + 
				"inner join Perfiles p on p.id = up.idPerfil " + 
				"where u.username = ?");
		
	}*/
	
	
	/*
	@Bean
	protected void configureUsers(HttpSecurity http) throws Exception {
		http.authorizeRequests()
		// Los recursos estáticos no requieren autenticación
		.antMatchers(
			"/bootstrap/**", 
			"/images/**",
			"/tinymce/**",
			"/logos/**").permitAll()
		// Las vistas públicas no requieren autenticación
		.antMatchers("/", 
			"/signup",
			"/search",
			"/vacantes/view/**").permitAll()
		// Todas las demás URLs de la Aplicación requieren autenticación
		.anyRequest().authenticated()
		// El formulario de Login no requiere autenticacion
		.and().formLogin().permitAll();
	}*/
	
	
	@Bean
	protected SecurityFilterChain filterchain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests()
			.requestMatchers("/bootstrap/**", "/images/**", "/tinymce/**", "/logos/**").permitAll()
			.requestMatchers("/", "/signup", "/search","/bcrypt/**", "/vacantes/view/**").permitAll()
			
			// Asignar permisos a URLs por ROLES
			//.requestMatchers("/solicitudes/indexPaginate/**").hasAnyAuthority("USUARIO")
			.requestMatchers("/solicitudes/save/**").hasAnyAuthority("USUARIO")
			.requestMatchers("/solicitudes/create/**").hasAnyAuthority("USUARIO")
			.requestMatchers("/solicitudes/**").hasAnyAuthority("SUPERVISOR","ADMINISTRADOR","USUARIO")
			.requestMatchers("/vacantes/**").hasAnyAuthority("SUPERVISOR","ADMINISTRADOR")
			.requestMatchers("/categorias/**").hasAnyAuthority("SUPERVISOR","ADMINISTRADOR") 
			.requestMatchers("/usuarios/**").hasAnyAuthority("ADMINISTRADOR")
			.requestMatchers("/solicitudes/indexPaginate_usuario/**").hasAnyAuthority("USUARIO")
			.anyRequest().authenticated()
			.and().formLogin().loginPage("/login").permitAll(); //.loginPage("/login") -> utilizare mi propio form login
		
		return http.build();
	}
	
	
	//password encrypted
	@Bean
	protected PasswordEncoder passwordEncoder() { 
	return new BCryptPasswordEncoder();
	}

		
}


