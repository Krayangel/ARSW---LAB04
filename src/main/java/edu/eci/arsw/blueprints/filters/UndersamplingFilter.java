package edu.eci.arsw.blueprints.filters;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
/**
 * Undersampling: conserva 1 de cada 2 puntos (índices pares), reduciendo la densidad.
 * Perfil: "undersampling"
 */
@Component
@Profile("undersampling")
public class UndersamplingFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) {
        List<Point> in = bp.getPoints();
        if (in.size() <= 2) return bp;
        
        List<Point> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            if (i % 2 == 0) { // Conserva índices pares
                out.add(in.get(i));
            }
        }
        
        return new Blueprint(bp.getAuthor(), bp.getName(), out);
    }
}