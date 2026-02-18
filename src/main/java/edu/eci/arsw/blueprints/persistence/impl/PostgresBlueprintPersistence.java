package edu.eci.arsw.blueprints.persistence.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Implementación de BlueprintPersistence utilizando PostgreSQL.
 * Almacena los blueprints en una tabla con columna JSONB para los puntos.
 * Se activa con el perfil "postgres".
 */
@Repository
@Profile("postgres")
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @Autowired
    public PostgresBlueprintPersistence(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * RowMapper para convertir una fila de la tabla blueprint en un objeto Blueprint.
     */
    private final RowMapper<Blueprint> blueprintRowMapper = (rs, rowNum) -> {
        String author = rs.getString("author");
        String name = rs.getString("name");
        String pointsJson = rs.getString("points");
        try {
            List<Point> points = objectMapper.readValue(pointsJson, new TypeReference<List<Point>>() {});
            return new Blueprint(author, name, points);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializando puntos del blueprint: " + author + "/" + name, e);
        }
    };

    @Override
    @Transactional
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        String sql = "INSERT INTO blueprint (author, name, points) VALUES (?, ?, ?::jsonb)";
        try {
            String pointsJson = objectMapper.writeValueAsString(bp.getPoints());
            jdbcTemplate.update(sql, bp.getAuthor(), bp.getName(), pointsJson);
        } catch (Exception e) {
            throw new BlueprintPersistenceException("Error guardando blueprint: " + e.getMessage());
        }
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        String sql = "SELECT author, name, points FROM blueprint WHERE author = ? AND name = ?";
        List<Blueprint> blueprints = jdbcTemplate.query(sql, blueprintRowMapper, author, name);
        if (blueprints.isEmpty()) {
            throw new BlueprintNotFoundException("Blueprint no encontrado: " + author + "/" + name);
        }
        return blueprints.get(0);
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        String sql = "SELECT author, name, points FROM blueprint WHERE author = ?";
        List<Blueprint> blueprints = jdbcTemplate.query(sql, blueprintRowMapper, author);
        if (blueprints.isEmpty()) {
            throw new BlueprintNotFoundException("No se encontraron blueprints para el autor: " + author);
        }
        return Set.copyOf(blueprints);
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        String sql = "SELECT author, name, points FROM blueprint";
        List<Blueprint> blueprints = jdbcTemplate.query(sql, blueprintRowMapper);
        return Set.copyOf(blueprints);
    }

    @Override
    @Transactional
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        // Versión mejorada: operación atómica en la base de datos usando concatenación JSONB
        String sql = "UPDATE blueprint SET points = points || ?::jsonb WHERE author = ? AND name = ?";
        try {
            // Creamos un objeto JSON con el nuevo punto
            String newPointJson = objectMapper.writeValueAsString(new Point(x, y));
            int updated = jdbcTemplate.update(sql, "[" + newPointJson + "]", author, name);
            if (updated == 0) {
                throw new BlueprintNotFoundException("Blueprint no encontrado: " + author + "/" + name);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando puntos del blueprint: " + e.getMessage(), e);
        }
    }
}