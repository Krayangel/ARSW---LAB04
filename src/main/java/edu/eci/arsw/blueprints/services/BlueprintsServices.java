package edu.eci.arsw.blueprints.services;

import java.util.Set;

import org.springframework.stereotype.Service;

import edu.eci.arsw.blueprints.filters.BlueprintsFilter;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;

@Service
public class BlueprintsServices {

    private final BlueprintPersistence persistence;
    private final BlueprintsFilter filter;

    public BlueprintsServices(BlueprintPersistence persistence, BlueprintsFilter filter) {
        this.persistence = persistence;
        this.filter = filter;
    }

    public void addNewBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (bp == null) {
            throw new BlueprintPersistenceException("El blueprint no puede ser nulo");
        }
        if (bp.getAuthor() == null || bp.getAuthor().trim().isEmpty()) {
            throw new BlueprintPersistenceException("El autor no puede estar vacío");
        }
        if (bp.getName() == null || bp.getName().trim().isEmpty()) {
            throw new BlueprintPersistenceException("El nombre no puede estar vacío");
        }
        if (bp.getPoints() == null || bp.getPoints().isEmpty()) {
            throw new BlueprintPersistenceException("El blueprint debe tener al menos un punto");
        }
        persistence.saveBlueprint(bp);
    }

    public Set<Blueprint> getAllBlueprints() {
        return persistence.getAllBlueprints();
    }

    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        if (author == null || author.trim().isEmpty()) {
            throw new BlueprintNotFoundException("El autor no puede estar vacío");
        }
        return persistence.getBlueprintsByAuthor(author);
    }

    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        if (author == null || author.trim().isEmpty()) {
            throw new BlueprintNotFoundException("El autor no puede estar vacío");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new BlueprintNotFoundException("El nombre no puede estar vacío");
        }
        return filter.apply(persistence.getBlueprint(author, name));
    }

    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        if (author == null || author.trim().isEmpty()) {
            throw new BlueprintNotFoundException("El autor no puede estar vacío");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new BlueprintNotFoundException("El nombre no puede estar vacío");
        }
        persistence.addPoint(author, name, x, y);
    }
}