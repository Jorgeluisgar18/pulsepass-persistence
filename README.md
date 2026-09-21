# PulsePass Persistence

Proyecto académico de persistencia para una plataforma de eventos,
artistas, usuarios y entradas.

El proyecto fue desarrollado con Java 21, Spring Boot 4,
Spring Data JPA, PostgreSQL, Flyway y Testcontainers.

## Tecnologías

- Java 21
- Spring Boot 4
- Maven
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Testcontainers
- JUnit 5
- AssertJ

## Modelo de dominio

El modelo principal está compuesto por las siguientes entidades:

### Venue

Representa un lugar donde se realizan eventos.

Relación:

Venue 1 ---- N Event

Un Venue puede tener varios eventos y cada Event pertenece
exactamente a un Venue.

### Event

Representa un evento disponible dentro de PulsePass.

Cada evento posee información como:

- código del evento
- nombre
- descripción
- categoría
- estado
- fecha
- edad mínima
- URL de streaming
- venue

Relaciones:

Event N ---- M Artist

Event 1 ---- N Ticket

### Artist

Representa los artistas participantes en los eventos.

La relación entre Event y Artist es muchos a muchos y se implementa
mediante la tabla intermedia:

event_artists

### User

Representa un usuario registrado en la plataforma.

Relaciones:

User 1 ---- 1 UserProfile

User 1 ---- N Ticket

### UserProfile

Contiene la información personal complementaria de un usuario.

La columna user_id posee una restricción UNIQUE para garantizar
la relación uno a uno.

### Ticket

Representa una entrada adquirida o reservada por un usuario.

Ticket se modela como una entidad independiente porque contiene
información propia:

- ticketCode
- type
- price
- status
- purchaseDate

Relaciones:

Ticket N ---- 1 User

Ticket N ---- 1 Event

Los valores monetarios utilizan BigDecimal y PostgreSQL NUMERIC.

## Enumeraciones

El proyecto utiliza las siguientes enumeraciones:

EventCategory:

- MUSIC
- SPORTS
- TECHNOLOGY
- EDUCATION
- CULTURE
- ENTERTAINMENT

EventStatus:

- DRAFT
- PUBLISHED
- SOLD_OUT
- CANCELLED
- FINISHED

TicketType:

- GENERAL
- VIP
- BACKSTAGE
- STUDENT

TicketStatus:

- RESERVED
- PAID
- CANCELLED
- USED

Las enumeraciones se almacenan utilizando EnumType.STRING.

## Persistencia

Todos los repositorios extienden JpaRepository.

Se utilizan Query Methods para consultas simples y navegación
entre relaciones.

También se utiliza JPQL para consultas que requieren JOIN,
COUNT, múltiples asociaciones y filtros más complejos.

Entre las consultas implementadas se encuentran:

- búsqueda de Venue por código
- búsqueda de Event por eventCode
- búsqueda de User por email ignorando mayúsculas y minúsculas
- eventos publicados ordenados por fecha
- eventos por código del Venue
- tickets por email del usuario y estado
- eventos asociados a un artista
- tickets pagados de un evento
- conteo de tickets pagados
- eventos por ciudad y artista
- eventos recomendados

No se utiliza SQL nativo para las consultas de repositorio.

## Migraciones Flyway

El esquema de la base de datos es administrado exclusivamente
por Flyway.

Hibernate se encuentra configurado con:

spring.jpa.hibernate.ddl-auto=validate

Esto permite que Hibernate valide el esquema sin crearlo ni
modificarlo.

Las migraciones son:

### V1__create_schema.sql

Crea las tablas:

- venues
- events
- artists
- event_artists
- users
- user_profiles
- tickets

También crea las restricciones PK, FK, UNIQUE, CHECK e índices.

### V2__insert_initial_artists.sql

Inserta el catálogo inicial de artistas:

- Solar Beat
- Neon Waves
- Caribbean Sound
- Ocean Drive
- Digital Pulse

### V3__add_streaming_url_to_event.sql

Agrega posteriormente la columna:

streaming_url VARCHAR(500)

a la tabla events.

Esto permite demostrar la evolución versionada del esquema.

## Pruebas de integración

Las pruebas utilizan Testcontainers para iniciar una instancia
real de PostgreSQL dentro de Docker.

Durante las pruebas:

1. Testcontainers inicia PostgreSQL.
2. Flyway ejecuta las migraciones V1, V2 y V3.
3. Hibernate valida el esquema.
4. Spring Data JPA ejecuta las operaciones de persistencia.
5. Se validan relaciones, consultas y restricciones.

Las pruebas comprueban:

- ejecución de las migraciones Flyway
- Venue 1:N Event
- Event N:M Artist
- User 1:1 UserProfile
- Ticket asociado a User y Event
- Query Methods
- consultas JPQL
- COUNT de tickets pagados
- restricciones UNIQUE reales en PostgreSQL

## Ejecución de las pruebas

Es necesario tener Docker Desktop iniciado.

Desde la raíz del proyecto ejecutar:

```bash
mvn clean test

```

El resultado esperado es:

```text
BUILD SUCCESS
```

## Alcance del proyecto

El proyecto se concentra exclusivamente en la capa de persistencia.

No incluye API REST, capa Service, frontend, autenticación,
pagos, códigos QR ni notificaciones.