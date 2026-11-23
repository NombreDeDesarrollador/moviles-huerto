package com.example.huertolindo_backend.service;

import com.example.huertolindo_backend.dto.ApiDtos.*;
import com.example.huertolindo_backend.exception.NotFoundException;
import com.example.huertolindo_backend.model.*;
import com.example.huertolindo_backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class HuertoService {
    private final UsuarioRepository usuarios;
    private final SectorRepository sectores;
    private final CultivoRepository cultivos;
    private final TareaRepository tareas;
    private final ActividadTareaRepository actividades;
    private final EvidenciaRepository evidencias;
    private final ReporteProblemaRepository reportes;
    private final ValidacionRepository validaciones;
    private final PasswordEncoder passwordEncoder;

    public HuertoService(UsuarioRepository usuarios, SectorRepository sectores, CultivoRepository cultivos,
                         TareaRepository tareas, ActividadTareaRepository actividades,
                         EvidenciaRepository evidencias, ReporteProblemaRepository reportes,
                         ValidacionRepository validaciones, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.sectores = sectores;
        this.cultivos = cultivos;
        this.tareas = tareas;
        this.actividades = actividades;
        this.evidencias = evidencias;
        this.reportes = reportes;
        this.validaciones = validaciones;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> usuarios(Rol rol) {
        return (rol == null ? usuarios.findAll() : usuarios.findByRol(rol)).stream().map(HuertoService::usuarioDto).toList();
    }

    public UsuarioResponse usuario(Long id) {
        return usuarioDto(findUsuario(id));
    }

    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        if (usuarios.existsByCorreo(request.correo())) throw new IllegalArgumentException("Correo ya registrado");
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        return usuarioDto(usuarios.save(usuario));
    }

    public UsuarioResponse actualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = findUsuario(id);
        usuarios.findByCorreo(request.correo()).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalArgumentException("Correo ya registrado"); });
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return usuarioDto(usuarios.save(usuario));
    }

    public void eliminarUsuario(Long id) {
        usuarios.delete(findUsuario(id));
    }

    public List<SectorResponse> sectores() {
        return sectores.findAll().stream().map(HuertoService::sectorDto).toList();
    }

    public SectorResponse sector(Long id) {
        return sectorDto(findSector(id));
    }

    public SectorResponse crearSector(SectorRequest request) {
        Sector sector = new Sector();
        aplicar(sector, request);
        return sectorDto(sectores.save(sector));
    }

    public SectorResponse actualizarSector(Long id, SectorRequest request) {
        Sector sector = findSector(id);
        aplicar(sector, request);
        return sectorDto(sectores.save(sector));
    }

    public void eliminarSector(Long id) {
        sectores.delete(findSector(id));
    }

    public List<CultivoResponse> cultivos(Long sectorId) {
        return (sectorId == null ? cultivos.findAll() : cultivos.findBySectorId(sectorId))
                .stream().map(HuertoService::cultivoDto).toList();
    }

    public CultivoResponse cultivo(Long id) {
        return cultivoDto(findCultivo(id));
    }

    public CultivoResponse crearCultivo(CultivoRequest request) {
        Cultivo cultivo = new Cultivo();
        aplicar(cultivo, request);
        return cultivoDto(cultivos.save(cultivo));
    }

    public CultivoResponse actualizarCultivo(Long id, CultivoRequest request) {
        Cultivo cultivo = findCultivo(id);
        aplicar(cultivo, request);
        return cultivoDto(cultivos.save(cultivo));
    }

    public CultivoResponse cambiarEstadoCultivo(Long id, EstadoCultivoRequest request) {
        Cultivo cultivo = findCultivo(id);
        cultivo.setEstado(request.estado());
        return cultivoDto(cultivos.save(cultivo));
    }

    public void eliminarCultivo(Long id) {
        cultivos.delete(findCultivo(id));
    }

    public List<TareaResponse> tareas(Long usuarioId, Long cultivoId, String correoActual, boolean admin) {
        List<Tarea> resultado;
        if (cultivoId != null) resultado = tareas.findByCultivoId(cultivoId);
        else if (usuarioId != null) resultado = tareas.findByResponsableId(usuarioId);
        else if (admin) resultado = tareas.findAll();
        else resultado = tareas.findByResponsableId(findUsuario(correoActual).getId());
        if (!admin) {
            Long idActual = findUsuario(correoActual).getId();
            resultado = resultado.stream().filter(tarea -> tarea.getResponsable().getId().equals(idActual)).toList();
        }
        return resultado.stream().map(HuertoService::tareaDto).toList();
    }

    public TareaResponse tarea(Long id) {
        return tareaDto(findTarea(id));
    }

    public TareaResponse tarea(Long id, String correoActual, boolean admin) {
        Tarea tarea = findTarea(id);
        verificarResponsable(tarea, correoActual, admin);
        return tareaDto(tarea);
    }

    public TareaResponse crearTarea(TareaRequest request) {
        Tarea tarea = new Tarea();
        aplicar(tarea, request);
        return tareaDto(tareas.save(tarea));
    }

    public TareaResponse actualizarTarea(Long id, TareaRequest request) {
        Tarea tarea = findTarea(id);
        aplicar(tarea, request);
        return tareaDto(tareas.save(tarea));
    }

    public void eliminarTarea(Long id) {
        tareas.delete(findTarea(id));
    }

    public TareaResponse completarTarea(Long id, String correoActual, boolean admin) {
        Tarea tarea = findTarea(id);
        verificarResponsable(tarea, correoActual, admin);
        tarea.setCompletada(true);
        return tareaDto(tareas.save(tarea));
    }

    public ActividadResponse agregarActividad(Long tareaId, ActividadRequest request, String correoActual,
                                               boolean admin) {
        Tarea tarea = findTarea(tareaId);
        Usuario usuario = findUsuario(correoActual);
        verificarResponsable(tarea, correoActual, admin);
        ActividadTarea actividad = new ActividadTarea();
        actividad.setDescripcion(request.descripcion());
        actividad.setFechaRegistro(LocalDateTime.now());
        actividad.setTarea(tarea);
        actividad.setUsuario(usuario);
        return actividadDto(actividades.save(actividad));
    }

    public EvidenciaResponse crearEvidencia(Long tareaId, String comentario, String ruta, String correoActual,
                                             boolean admin) {
        Tarea tarea = findTarea(tareaId);
        verificarResponsable(tarea, correoActual, admin);
        Evidencia evidencia = new Evidencia();
        evidencia.setTarea(tarea);
        evidencia.setUsuario(findUsuario(correoActual));
        evidencia.setComentario(comentario);
        evidencia.setRutaImagen(ruta);
        evidencia.setFechaSubida(LocalDateTime.now());
        return evidenciaDto(evidencias.save(evidencia));
    }

    public void validarAccesoTarea(Long tareaId, String correoActual, boolean admin) {
        verificarResponsable(findTarea(tareaId), correoActual, admin);
    }

    public List<EvidenciaResponse> evidencias() {
        return evidencias.findAll().stream().map(HuertoService::evidenciaDto).toList();
    }

    public List<ReporteResponse> reportes() {
        return reportes.findAll().stream().map(HuertoService::reporteDto).toList();
    }

    public ReporteResponse reporte(Long id) {
        return reporteDto(findReporte(id));
    }

    public ReporteResponse crearReporte(ReporteRequest request, String correoActual, boolean admin) {
        Usuario autor = findUsuario(request.usuarioId());
        if (!admin && !autor.getCorreo().equals(correoActual)) {
            throw new org.springframework.security.access.AccessDeniedException("No puedes reportar en nombre de otro usuario");
        }
        ReporteProblema reporte = new ReporteProblema();
        aplicar(reporte, request, autor);
        return reporteDto(reportes.save(reporte));
    }

    public ReporteResponse actualizarReporte(Long id, ReporteRequest request, String correoActual, boolean admin) {
        ReporteProblema reporte = findReporte(id);
        if (!admin && !reporte.getUsuario().getCorreo().equals(correoActual)) {
            throw new org.springframework.security.access.AccessDeniedException("No puedes modificar este reporte");
        }
        aplicar(reporte, request, findUsuario(request.usuarioId()));
        return reporteDto(reportes.save(reporte));
    }

    public void eliminarReporte(Long id) {
        reportes.delete(findReporte(id));
    }

    public ReporteResponse resolverReporte(Long id) {
        ReporteProblema reporte = findReporte(id);
        reporte.setResuelto(true);
        return reporteDto(reportes.save(reporte));
    }

    public ValidacionResponse crearValidacion(ValidacionRequest request, String correoValidador) {
        if ((request.tareaId() == null) == (request.evidenciaId() == null)) {
            throw new IllegalArgumentException("Debe especificar exactamente una tarea o evidencia");
        }
        Validacion validacion = new Validacion();
        validacion.setAprobado(request.aprobado());
        validacion.setObservacion(request.observacion());
        validacion.setFechaValidacion(LocalDateTime.now());
        validacion.setValidador(findUsuario(correoValidador));
        if (request.tareaId() != null) validacion.setTarea(findTarea(request.tareaId()));
        if (request.evidenciaId() != null) {
            validacion.setEvidencia(evidencias.findById(request.evidenciaId())
                    .orElseThrow(() -> new NotFoundException("Evidencia no encontrada")));
        }
        return validacionDto(validaciones.save(validacion));
    }

    public List<ValidacionResponse> validaciones() {
        return validaciones.findAll().stream().map(HuertoService::validacionDto).toList();
    }

    private void aplicar(Sector sector, SectorRequest request) {
        sector.setNombre(request.nombre());
        sector.setDescripcion(request.descripcion());
        sector.setUbicacion(request.ubicacion());
    }

    private void aplicar(Cultivo cultivo, CultivoRequest request) {
        cultivo.setNombre(request.nombre());
        cultivo.setEspecie(request.especie());
        cultivo.setFechaSiembra(request.fechaSiembra());
        cultivo.setEstado(request.estado());
        cultivo.setSector(findSector(request.sectorId()));
    }

    private void aplicar(Tarea tarea, TareaRequest request) {
        tarea.setTitulo(request.titulo());
        tarea.setDescripcion(request.descripcion());
        tarea.setTipo(request.tipo());
        tarea.setFechaProgramada(request.fechaProgramada());
        tarea.setCompletada(request.completada());
        tarea.setCultivo(findCultivo(request.cultivoId()));
        tarea.setResponsable(findUsuario(request.responsableId()));
    }

    private void aplicar(ReporteProblema reporte, ReporteRequest request, Usuario autor) {
        reporte.setTitulo(request.titulo());
        reporte.setDescripcion(request.descripcion());
        reporte.setGravedad(request.gravedad());
        reporte.setCultivo(findCultivo(request.cultivoId()));
        reporte.setUsuario(autor);
    }

    private void verificarResponsable(Tarea tarea, String correoActual, boolean admin) {
        if (!admin && !tarea.getResponsable().getCorreo().equals(correoActual)) {
            throw new org.springframework.security.access.AccessDeniedException("La tarea no está asignada a este usuario");
        }
    }

    private Usuario findUsuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private Usuario findUsuario(String correo) {
        return usuarios.findByCorreo(correo).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private Sector findSector(Long id) {
        return sectores.findById(id).orElseThrow(() -> new NotFoundException("Sector no encontrado"));
    }

    private Cultivo findCultivo(Long id) {
        return cultivos.findById(id).orElseThrow(() -> new NotFoundException("Cultivo no encontrado"));
    }

    private Tarea findTarea(Long id) {
        return tareas.findById(id).orElseThrow(() -> new NotFoundException("Tarea no encontrada"));
    }

    private ReporteProblema findReporte(Long id) {
        return reportes.findById(id).orElseThrow(() -> new NotFoundException("Reporte no encontrado"));
    }

    private static UsuarioResponse usuarioDto(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.isActivo());
    }

    private static SectorResponse sectorDto(Sector s) {
        return new SectorResponse(s.getId(), s.getNombre(), s.getDescripcion(), s.getUbicacion());
    }

    private static CultivoResponse cultivoDto(Cultivo c) {
        return new CultivoResponse(c.getId(), c.getNombre(), c.getEspecie(), c.getFechaSiembra(),
                c.getEstado(), c.getSector().getId(), c.getSector().getNombre());
    }

    private static TareaResponse tareaDto(Tarea t) {
        return new TareaResponse(t.getId(), t.getTitulo(), t.getDescripcion(), t.getTipo(),
                t.getFechaProgramada(), t.isCompletada(), t.getCultivo().getId(), t.getCultivo().getNombre(),
                t.getResponsable().getId(), t.getResponsable().getNombre());
    }

    private static ActividadResponse actividadDto(ActividadTarea a) {
        return new ActividadResponse(a.getId(), a.getDescripcion(), a.getFechaRegistro(), a.getTarea().getId(),
                a.getUsuario().getId(), a.getUsuario().getNombre());
    }

    private static EvidenciaResponse evidenciaDto(Evidencia e) {
        return new EvidenciaResponse(e.getId(), e.getRutaImagen(), e.getComentario(), e.getFechaSubida(),
                e.getTarea().getId(), e.getUsuario().getId(), e.getUsuario().getNombre());
    }

    private static ReporteResponse reporteDto(ReporteProblema r) {
        return new ReporteResponse(r.getId(), r.getTitulo(), r.getDescripcion(), r.getGravedad(), r.isResuelto(),
                r.getFechaReporte(), r.getCultivo().getId(), r.getCultivo().getNombre(),
                r.getUsuario().getId(), r.getUsuario().getNombre());
    }

    private static ValidacionResponse validacionDto(Validacion v) {
        return new ValidacionResponse(v.getId(), v.isAprobado(), v.getObservacion(), v.getFechaValidacion(),
                v.getValidador().getId(), v.getValidador().getNombre(),
                v.getTarea() == null ? null : v.getTarea().getId(),
                v.getEvidencia() == null ? null : v.getEvidencia().getId());
    }
}
