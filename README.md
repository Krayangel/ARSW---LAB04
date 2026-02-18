## Laboratorio #4 – REST API Blueprints (Java 21 / Spring Boot 3.3.x)
# Escuela Colombiana de Ingeniería – Arquitecturas de Software  

---

## 📋 Requisitos
- Java 21 
- Maven 3.9+

## ▶️ Ejecución del proyecto
```bash
mvn clean install
mvn spring-boot:run
```
Probar con `curl`:
```bash
curl -s http://localhost:8080/blueprints | jq
curl -s http://localhost:8080/blueprints/john | jq
curl -s http://localhost:8080/blueprints/john/house | jq
curl -i -X POST http://localhost:8080/blueprints -H 'Content-Type: application/json' -d '{ "author":"john","name":"kitchen","points":[{"x":1,"y":1},{"x":2,"y":2}] }'
curl -i -X PUT  http://localhost:8080/blueprints/john/kitchen/points -H 'Content-Type: application/json' -d '{ "x":3,"y":3 }'
```

> Si deseas activar filtros de puntos (reducción de redundancia, *undersampling*, etc.), implementa nuevas clases que implementen `BlueprintsFilter` y cámbialas por `IdentityFilter` con `@Primary` o usando configuración de Spring.
---

Abrir en navegador:  
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)  

---

## 🗂️ Estructura de carpetas (arquitectura)

```
src/main/java/edu/eci/arsw/blueprints
  ├── model/         # Entidades de dominio: Blueprint, Point
  ├── persistence/   # Interfaz + repositorios (InMemory, Postgres)
  │    └── impl/     # Implementaciones concretas
  ├── services/      # Lógica de negocio y orquestación
  ├── filters/       # Filtros de procesamiento (Identity, Redundancy, Undersampling)
  ├── controllers/   # REST Controllers (BlueprintsAPIController)
  └── config/        # Configuración (Swagger/OpenAPI, etc.)
```

> Esta separación sigue el patrón **capas lógicas** (modelo, persistencia, servicios, controladores), facilitando la extensión hacia nuevas tecnologías o fuentes de datos.

---

## 📖 Actividades del laboratorio

### 1. Familiarización con el código base
- Revisa el paquete `model` con las clases `Blueprint` y `Point`.  

    Respuesta: La clase de point es un record, es inmutable y que presenta un par de coordenadas, una X y otra Y, la clase de BBlueprint modela un plano con un autor, nomre, y analisis de puntos, presenta metodos para acceder a sus atribbutos y agregar nuevos puntos.

    Record : es un tipo especial de clase diseñado para almacenar datos de forma inmutable de manera concisa y segura.

    Point tiene:

      - Constructor canónico con los parámetros int x, int y.
      - Métodos de acceso x() e y().
      - Implementaciones de equals(), hashCode() y toString() basadas en los componentes.

    Bleprint tiene:
      - Atributos: author (String), name (String) y una lista points de tipo List<Point> inicializada como ArrayList vacío.
      - Constructor: recibe autor, nombre y una lista de puntos; si la lista no es null, agrega todos los puntos a la lista interna. Esto permite crear un Blueprint con puntos iniciales.
      - Métodos de acceso: getAuthor(), getName() y getPoints() (este último devuelve una vista no modificable de la lista para mantener la encapsulación).
      - Método addPoint(Point p): agrega un nuevo punto a la lista interna.
      - Métodos equals() y hashCode(): están basados únicamente en author y name, lo que significa que dos Blueprint se consideran iguales si tienen el mismo autor y nombre, independientemente de los puntos. Esto es clave para la persistencia en un mapa donde la clave es la combinación autor:nombre.

- Entiende la capa `persistence` con `InMemoryBlueprintPersistence`.  

    InMemoryBlueprintPersistence es una implementación en memoria de la interfaz BlueprintPersistence, esta utiliza un ConcurrentHashMap para almacenar los blueprints con una clave compuesta "autor:nombre", incluye datos de ejemplo precargados y proporciona operaciones CRUD básicas, lanzando excepciones personalizadas cuando un blueprint no existe o ya está presente.

    Este usa:

      Interfaz BlueprintPersistence
      Define los contratos que debe cumplir cualquier implementación de persistencia:
      - saveBlueprint(Blueprint bp): guarda un nuevo blueprint, lanza BlueprintPersistenceException si ya existe.
      - getBlueprint(String author, String name): obtiene un blueprint por autor y nombre, lanza BlueprintNotFoundException si no existe.
      - getBlueprintsByAuthor(String author): devuelve un Set de blueprints de un autor, lanza excepción si no hay ninguno.
      - getAllBlueprints(): devuelve todos los blueprints almacenados.
      - addPoint(String author, String name, int x, int y): agrega un punto a un blueprint existente.

      InMemoryBlueprintPersistence
      Está anotada con @Repository, lo que la convierte en un bean de Spring y permite inyectarla en servicios.

      * Estructura de almacenamiento: Map<String, Blueprint> blueprints = new ConcurrentHashMap<>() garantiza hilo-safety para operaciones concurrentes.
      - Método auxiliar keyOf: genera la clave en formato "autor:nombre".
      
      - Constructor: precarga tres blueprints de ejemplo:
        - john:house con cuatro puntos.
        - john:garage con tres puntos.
        - jane:garden con tres puntos.

      - Implementación de métodos:
        - saveBlueprint: verifica si la clave ya existe; si es así, lanza excepción; de lo contrario, guarda.
        - getBlueprint: obtiene del mapa; si es null, lanza BlueprintNotFoundException.
        - getBlueprintsByAuthor: filtra por autor usando un stream; si el conjunto resultante está vacío, lanza excepción.
        - getAllBlueprints: retorna una copia en un nuevo HashSet.
        - addPoint: obtiene el blueprint (puede lanzar excepción) y luego le agrega el punto.

- Analiza la capa `services` (`BlueprintsServices`) y el controlador `BlueprintsAPIController`.

    BlueprintsServices es la fachada de lógica de negocio; inyecta la persistencia y un filtro (BlueprintsFilter) para aplicar transformaciones a los blueprints al consultarlos. El controlador expone endpoints REST que delegan en el servicio, manejan excepciones y devuelven respuestas HTTP adecuadas (200, 201, 404, 403, etc.). Juntos forman la API REST completa para gestionar blueprints.

    Datos:

      - Anotación: @Service – indica que es un bean de servicio de Spring.
      - Dependencias: recibe por constructor BlueprintPersistence y BlueprintsFilter. Esto sigue el principio de inyección de dependencias, facilitando pruebas y desacoplamiento.

    Métodos:
    - addNewBlueprint(Blueprint bp): simplemente llama a persistence.saveBlueprint(bp). Puede lanzar BlueprintPersistenceException.
    - getAllBlueprints(): retorna todos los blueprints sin aplicar filtro (importante: según el código actual, no filtra; solo llama a persistencia).
    - getBlueprintsByAuthor(String author): retorna los blueprints del autor sin filtrar (solo persistencia).
    - getBlueprint(String author, String name): obtiene el blueprint de persistencia y luego aplica el filtro (filter.apply(bp)) antes de devolverlo. Esto permite modificar la lista de puntos (ej. eliminar redundancias, submuestrear) según el filtro activo.
    - addPoint(String author, String name, int x, int y): delega en persistencia para agregar un punto.


    BlueprintsAPIController:

      - Anotaciones: @RestController y @RequestMapping("/blueprints").
      - Dependencia: recibe BlueprintsServices por constructor.
      - Manejo de excepciones: cada método captura las excepciones específicas y devuelve un ResponseEntity con el código HTTP adecuado y un cuerpo JSON con mensaje de error.

### 2. Migración a persistencia en PostgreSQL
- Configura una base de datos PostgreSQL (puedes usar Docker).  
- Implementa un nuevo repositorio `PostgresBlueprintPersistence` que reemplace la versión en memoria.  
- Mantén el contrato de la interfaz `BlueprintPersistence`.  

    Resolucion del problema:
      Se instaló PostgreSQL localmente, donde luego se creó la ase de datos llamada 'blueprintsdb' y se implementó PostgresBlueprintPersistence usando JdbcTemplate y almacenando los puntos como JSONB. Se configuraron perfiles Spring (postgres/memory) para poder cambiar entre implementaciones.

### 3. Buenas prácticas de API REST
- Cambia el path base de los controladores a `/api/v1/blueprints`.  
- Usa **códigos HTTP** correctos:  
  - `200 OK` (consultas exitosas).  
  - `201 Created` (creación).  
  - `202 Accepted` (actualizaciones).  
  - `400 Bad Request` (datos inválidos).  
  - `404 Not Found` (recurso inexistente).  
- Implementa una clase genérica de respuesta uniforme:
  ```java
  public record ApiResponse<T>(int code, String message, T data) {}
  ```
  Ejemplo JSON:
  ```json
  {
    "code": 200,
    "message": "execute ok",
    "data": { "author": "john", "name": "house", "points": [...] }
  }
  ```

    Para resolver este ejercicio se realizaron los siguientes camios:

      Se modificó el @RequestMapping a /api/v1/blueprints, se implementó el record ApiResponse para uniformar respuestas, y se ajustaron los códigos HTTP según cada operación.

    1. Path base: Se cambió @RequestMapping("/blueprints") a @RequestMapping("/api/v1/blueprints").

    2. Clase ApiResponse: Se creó el record ApiResponse<T> con code, message y data, más métodos estáticos success, successWithoutData y error.

    3. Códigos HTTP:

            200 OK para GET exitosos.

            201 Created para POST exitoso.

            202 Accepted para PUT exitoso.

            400 Bad Request para datos inválidos.

            404 Not Found para recursos inexistentes.

            409 Conflict para intento de crear recurso duplicado.

    4. Validaciones: Se agregaron validaciones con @Valid y @NotBlank en los DTOs.

### 4. OpenAPI / Swagger
- Configura `springdoc-openapi` en el proyecto.  
- Expón documentación automática en `/swagger-ui.html`.  
- Anota endpoints con `@Operation` y `@ApiResponse`.

  Se agregó la dependencia springdoc-openapi, se configuró en application.properties y se anotó el controlador con @Tag, @Operation, @ApiResponses y @Parameter para documentar la API.

  1. Dependencia: Se verificó la dependencia springdoc-openapi-starter-webmvc-ui en el pom.xml.

  2. Configuración: En application.properties se definieron:

        springdoc.api-docs.path=/api-docs

        springdoc.swagger-ui.path=/swagger-ui.html

  3. Anotaciones en el controlador:

      @Tag(name = "Blueprints", description = "...") para agrupar endpoints.

      @Operation para describir cada método.

      @ApiResponses con @ApiResponse para documentar códigos de respuesta.

      @Parameter para documentar parámetros de path.

  4. Acceso: La documentación está disponible en http://localhost:8080/swagger-ui.html y el OpenAPI JSON en http://localhost:8080/api-docs.

### 5. Filtros de *Blueprints*
- Implementa filtros:
  - **RedundancyFilter**: elimina puntos duplicados consecutivos.  
  - **UndersamplingFilter**: conserva 1 de cada 2 puntos.  
- Activa los filtros mediante perfiles de Spring (`redundancy`, `undersampling`).  


  Se implementaron los filtros como componentes Spring con perfiles. Se modificó BlueprintsServices para aplicar el filtro en las consultas. En application.properties se activan múltiples perfiles separados por comas (ej: postgres,redundancy).

  1. Interfaz BlueprintsFilter: Define el método Blueprint apply(Blueprint bp).

  2. RedundancyFilter: Con perfil "redundancy". Elimina puntos consecutivos duplicados comparando coordenadas.

  3. UndersamplingFilter: Con perfil "undersampling". Conserva puntos en índices pares (0,2,4...).

  4. IdentityFilter: Con perfil "identity". Devuelve el blueprint sin cambios (fallback).

  5. BlueprintsServices: Inyecta el filtro y lo aplica en getAllBlueprints, getBlueprintsByAuthor y getBlueprint.

  6. Configuración de perfiles: En application.properties se definen perfiles múltiples:

      spring.profiles.active=postgres,redundancy

      Esto activa PostgreSQL y el filtro de redundancia simultáneamente.

  7. Pruebas: Se crearon blueprints con puntos duplicados y se verificó que el filtro actúa correctamente.


## ----------------
### Guia de uso
Requisitos previos
Java 21

Maven

PostgreSQL (con BD blueprintsdb creada)

Configuración inicial
Crear la base de datos en PostgreSQL:

sql
CREATE DATABASE blueprintsdb;
Crear la tabla blueprint (si no se usa schema.sql automático):

sql
CREATE TABLE IF NOT EXISTS blueprint (
    author VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    points JSONB NOT NULL,
    PRIMARY KEY (author, name)
);
Configurar application.properties con tus credenciales:

properties
spring.datasource.url=jdbc:postgresql://localhost:5432/blueprintsdb
spring.datasource.username=postgres
spring.datasource.password=TU_CONTRASEÑA
spring.profiles.active=postgres,redundancy  # O el filtro deseado
Compilar y ejecutar
bash
# Compilar (sin tests para evitar errores)
mvn clean install -DskipTests

# Ejecutar la aplicación
mvn spring-boot:run

Endpoints disponibles

Método	URL	Descripción

GET	/api/v1/blueprints	Obtener todos los blueprints

GET	/api/v1/blueprints/{author}	Obtener blueprints por autor

GET	/api/v1/blueprints/{author}/{bpname}	Obtener blueprint específico

POST	/api/v1/blueprints	Crear nuevo blueprint

PUT	/api/v1/blueprints/{author}/{bpname}/points	Agregar punto a blueprint

URLs importantes
Recurso	URL

API Base	http://localhost:8080/api/v1/blueprints

Swagger UI	http://localhost:8080/swagger-ui.html

OpenAPI JSON	http://localhost:8080/api-docs
## -----------------

---

## ✅ Entregables

1. Repositorio en GitHub con:  
   - Código fuente actualizado.  
   - Configuración PostgreSQL (`application.yml` o script SQL).  
   - Swagger/OpenAPI habilitado.  
   - Clase `ApiResponse<T>` implementada.  

2. Documentación:  
   - Informe de laboratorio con instrucciones claras.  
   - Evidencia de consultas en Swagger UI y evidencia de mensajes en la base de datos.  
   - Breve explicación de buenas prácticas aplicadas.  

---

## 📊 Criterios de evaluación

| Criterio | Peso |
|----------|------|
| Diseño de API (versionamiento, DTOs, ApiResponse) | 25% |
| Migración a PostgreSQL (repositorio y persistencia correcta) | 25% |
| Uso correcto de códigos HTTP y control de errores | 20% |
| Documentación con OpenAPI/Swagger + README | 15% |
| Pruebas básicas (unitarias o de integración) | 15% |

**Bonus**:  

- Imagen de contenedor (`spring-boot:build-image`).  
- Métricas con Actuator.  