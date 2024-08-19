package net.brubio.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import net.brubio.model.Solicitud;
import net.brubio.model.Usuario;

public interface SolicitudesRepository extends JpaRepository<Solicitud, Integer>{
	
	
	Page<Solicitud> findByUsuario(Usuario user, Pageable pageable);

	
}
