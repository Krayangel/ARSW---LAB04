package edu.eci.arsw.blueprints.controllers;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.eci.arsw.blueprints.dto.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "API para gestionar blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) {
        this.services = services;
    }

    @GetMapping
    @Operation(summary = "Obtener todos los blueprints", description = "Retorna una lista de todos los blueprints")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO aquí
            responseCode = "200", 
            description = "Lista obtenida exitosamente",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        Set<Blueprint> blueprints = services.getAllBlueprints();
        return ResponseEntity.ok(ApiResponse.success(blueprints));
    }

    @GetMapping("/{author}")
    @Operation(summary = "Obtener blueprints por autor", 
               description = "Retorna todos los blueprints de un autor específico")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "200", 
            description = "Blueprints encontrados"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "404", 
            description = "Autor no encontrado")
    })
    public ResponseEntity<ApiResponse<?>> getByAuthor(
            @Parameter(description = "Nombre del autor", required = true)
            @PathVariable String author) {
        try {
            Set<Blueprint> blueprints = services.getBlueprintsByAuthor(author);
            return ResponseEntity.ok(ApiResponse.success(blueprints));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @GetMapping("/{author}/{bpname}")
    @Operation(summary = "Obtener blueprint por autor y nombre", 
               description = "Retorna un blueprint específico")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "200", 
            description = "Blueprint encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "404", 
            description = "Blueprint no encontrado")
    })
    public ResponseEntity<ApiResponse<?>> getByAuthorAndName(
            @Parameter(description = "Nombre del autor", required = true)
            @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", required = true)
            @PathVariable String bpname) {
        try {
            Blueprint blueprint = services.getBlueprint(author, bpname);
            return ResponseEntity.ok(ApiResponse.success(blueprint));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo blueprint", 
               description = "Crea un nuevo blueprint con los puntos especificados")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "201", 
            description = "Blueprint creado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "400", 
            description = "Datos inválidos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "409", 
            description = "Blueprint ya existe")
    })
    public ResponseEntity<ApiResponse<Void>> addBlueprint(
            @Parameter(description = "Datos del blueprint", required = true)
            @Valid @RequestBody NewBlueprintRequest req) {
        
        if (req.points() == null || req.points().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "El blueprint debe tener al menos un punto"));
        }
        
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.successWithoutData(201, "Blueprint creado exitosamente"));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, e.getMessage()));
        }
    }

    @PutMapping("/{author}/{bpname}/points")
    @Operation(summary = "Agregar punto a blueprint", 
               description = "Agrega un nuevo punto a un blueprint existente")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "202", 
            description = "Punto agregado exitosamente"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "400", 
            description = "Punto inválido"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(  // Nombre COMPLETO
            responseCode = "404", 
            description = "Blueprint no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> addPoint(
            @Parameter(description = "Nombre del autor", required = true)
            @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", required = true)
            @PathVariable String bpname,
            @Parameter(description = "Punto a agregar", required = true)
            @RequestBody Point point) {
        
        if (point == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "El punto no puede ser nulo"));
        }
        
        try {
            services.addPoint(author, bpname, point.x(), point.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.successWithoutData(202, "Punto agregado exitosamente"));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        }
    }

    public record NewBlueprintRequest(
            @NotBlank(message = "El autor no puede estar vacío")
            String author,
            @NotBlank(message = "El nombre no puede estar vacío")
            String name,
            @NotNull(message = "La lista de puntos no puede ser nula")
            java.util.List<Point> points
    ) { }
}