package net.brubio.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;

import jakarta.servlet.http.HttpServletResponse;
import net.brubio.model.Perfil;
import net.brubio.model.Solicitud;
import net.brubio.model.Usuario;
import net.brubio.model.Vacante;
import net.brubio.service.ISolicitudesService;
import net.brubio.service.IUsuariosService;
import net.brubio.service.IVacantesService;
import net.brubio.util.Utileria;

@Controller
@RequestMapping("solicitudes")
public class SolicitudesController {
	
	@Value("${jobsapp.path.cv}")
	private String rutaCv;
	
	@Autowired
	private IVacantesService serviceVacantes;
	
	@Autowired
	private IUsuariosService serviceUsuarios;
	
	@Autowired
	private ISolicitudesService serviceSolicitudes;
	
	@Autowired
	private JavaMailSender javaMailSender;
	
	//set para almacenar los ID de las solicitudes ya notificadas
	//private Set<Integer> solicitudesNotificadas = ConcurrentHashMap.newKeySet();
	
	
	@GetMapping("/indexPaginate")
	public String mostrarIndexPaginado(Pageable pageable, Model model) {
		Page<Solicitud> lista = serviceSolicitudes.buscarTodas(pageable);
		model.addAttribute("listaSolicitudes", lista);
		return "solicitudes/listSolicitudes";
	}
	
	@GetMapping("/indexPaginate_usuario") 
	public String mostrarIndexPaginado(Pageable pageable, Model model, Principal principal, RedirectAttributes attributes) {
		String username = principal.getName();
		Usuario user = serviceUsuarios.buscarPorUsername(username);
		System.err.println(user);
		
		Page<Solicitud> lista = serviceSolicitudes.buscarPorUsuario(user, pageable);
		
		if(lista.isEmpty()) {
			System.err.println("lista de solicitudes vacia !");
			model.addAttribute("msg_null", "No hay solicitudes registradas para este usuario");
		}else {
			model.addAttribute("listaSolicitudes", lista);
		}
		
		/*for (Perfil perfil : user.getPerfiles()) {
            System.err.println("Perfil ID: " + perfil.getId() + ", Perfil: " + perfil.getPerfil());
            
            if(perfil.getPerfil().equals("USUARIO")) {
            	lista = serviceSolicitudes.buscarPorUsuario(user, pageable);
            }
            if(perfil.getPerfil().equals("ADMINISTRADOR")) {
            	lista = serviceSolicitudes.buscarTodas(pageable);
            }
            if(perfil.getPerfil().equals("SUPERVISOR") && perfil.getPerfil().equals("ADMINISTRADOR")) {
            	lista = serviceSolicitudes.buscarTodas(pageable);
            }
        } */
		
		//filtrar las solicitdes segun el rol
		//Page<Solicitud> lista= serviceSolicitudes.buscarTodas(pageable);
		//model.addAttribute("listaSolicitudes", lista);
		return "solicitudes/listSolicitudes";
	}
	
	
	@GetMapping("/create/{idVacante}")
	public String crear(Solicitud solicitud, @PathVariable("idVacante") Integer idVacante, Model model) {
		Vacante vacante = serviceVacantes.buscarVacantePorId(idVacante);
		System.out.println("id de la Vacante : " + idVacante);
		model.addAttribute("vacante", vacante);
		return "solicitudes/formSolicitud";
	}
	
	
	@PostMapping("/save")
	public String guardar(Solicitud solicitud, BindingResult result, @RequestParam("archivoCV") MultipartFile multipart, 
			Authentication autentication, RedirectAttributes attributes) {
		
		//recuperamos el nombre del usuario actual
		String username = autentication.getName();
		
		if(result.hasErrors()) {
			System.out.println("Existieron Errores");
			return "solicitudes/formSolicitud";
		}
		
		if(!multipart.isEmpty()) {
			String nombreArchivo = Utileria.guardarArchivo(multipart, rutaCv);
			if(nombreArchivo != null) {
				solicitud.setArchivo(nombreArchivo);
			}
		}
		
		//relacionamos al Usuario con la Solicitud
		Usuario usuario = serviceUsuarios.buscarPorUsername(username);
		solicitud.setUsuario(usuario);
		
		//guardamos en bd
		serviceSolicitudes.guardar(solicitud);
		attributes.addFlashAttribute("msg", "Gracias por enviar tu CV !!!");
		
		System.out.println("Solicitud: " + solicitud);
		return "redirect:/";
	}
	
	
	@GetMapping("/delete/{idSolicitud}")
	public String eliminar(@PathVariable("idSolicitud") Integer idSolicitud, RedirectAttributes attributes) {
		serviceSolicitudes.eliminar(idSolicitud);
		attributes.addFlashAttribute("msg", "Solcitud Eliminada !!!");
		return "redirect:/solicitudes/indexPaginate";
	}
	
	
	@GetMapping("/sendMail/{id}")
	public String enviarEmail(@PathVariable("id") Integer idSolicitud, RedirectAttributes attributes) {
		SimpleMailMessage message = new SimpleMailMessage();
		Solicitud solicitud = serviceSolicitudes.buscarPorId(idSolicitud);
		System.out.println(solicitud.getUsuario().getEmail());
		
		message.setTo(solicitud.getUsuario().getEmail());
		message.setSubject("Recepcion de CV");
		message.setText("La solucitud se envio satisfactoriamente");
		
		try {
			javaMailSender.send(message);
			attributes.addFlashAttribute("msg", "Correo enviado exitosamente.");
		} catch (Exception e) {
			attributes.addFlashAttribute("msg", "Error al enviar el correo.");
		}
		
		return "redirect:/solicitudes/indexPaginate";
	} 
	
	
	@GetMapping("/downloadCv/{archivo}/{id}")
	public String descargarCv(@PathVariable("archivo") String archivo, @PathVariable("id") Integer id,
			Authentication autentication, RedirectAttributes attributes) {
		
		System.err.println("download cv " + archivo);
		Solicitud solicitud = serviceSolicitudes.buscarPorId(id);
		System.err.println(solicitud);

		String username = autentication.getName();
		Usuario usuario = serviceUsuarios.buscarPorUsername(username);

		System.err.println(usuario);

		boolean esAdministrador = false;

		for (Perfil perfil : usuario.getPerfiles()) {
			if (perfil.getPerfil().equalsIgnoreCase("ADMINISTRADOR")) {
				esAdministrador = true;
				break;
			}
		}

		if (esAdministrador) {
			System.out.println("Es ADMINISTRADOR.");
			solicitud.setVista(true);

			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(solicitud.getUsuario().getEmail());
			message.setSubject("Tu CV fue visto");
			message.setText("Tu cv fue visualizado por uno de los administradores");
			javaMailSender.send(message);
			attributes.addFlashAttribute("msg", "Correo enviado exitosamente.");
			System.err.println("se envio el correo a " + solicitud.getUsuario().getEmail());
		} else {
			System.out.println("NO es ADMINISTRADOR.");
		}

		// return "redirect:/solicitudes/indexPaginate";
		return "redirect:/cv/" + archivo;
	}
	
	
	@GetMapping("/downloadCv_2/{archivo}/{id}")
	public void descargarCv_2(@PathVariable("archivo") String archivo, @PathVariable("id") Integer id,
			Authentication autentication, RedirectAttributes attributes, HttpServletResponse response) throws IOException {
		
		Solicitud solicitud = serviceSolicitudes.buscarPorId(id);
        String username = autentication.getName();
        Usuario usuario = serviceUsuarios.buscarPorUsername(username);
        
        boolean esAdministrador = false;

		for (Perfil perfil : usuario.getPerfiles()) {
			if (perfil.getPerfil().equalsIgnoreCase("ADMINISTRADOR")) {
				esAdministrador = true;
				break;
			}
		}
		
		// Prepara el archivo para la descarga
        File file = new File("c:/empleos/files-cv/" + archivo); // ruta
        if (file.exists()) {
            // Configura la respuesta HTTP para la descarga
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + archivo + "\"");
            
            // Stream del archivo al cliente
            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }// Se completo el streaming
        } else {// Manejo de error si el archivo no existe
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "El archivo no fue encontrado.");
        }
		
		System.err.println(solicitud.getVista());
		
		if (esAdministrador) {
            //verificar si el campo vista es false para mandar correo de cv visto
            if (!solicitud.getVista()) {
            	// Enviar el correo
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(solicitud.getUsuario().getEmail());
                message.setSubject("Tu CV fue visto");
                message.setText("Tu CV fue visualizado por uno de los administradores para el puesto de " + solicitud.getVacante().getNombre());
                javaMailSender.send(message);
                System.err.println("Se envió el correo a " + solicitud.getUsuario().getEmail());
                
                solicitud.setVista(true);
                serviceSolicitudes.guardar(solicitud); // Guardar cambios en la base de datos
                System.out.println(solicitud.getVista() + " **  La solicitud ha sido marcada como vista.");
            } else {
                System.out.println("El correo ya fue enviado anteriormente.");
            }
        } else {
            System.out.println("NO es ADMINISTRADOR.");
            //hola
        }

	}
	
		
}
