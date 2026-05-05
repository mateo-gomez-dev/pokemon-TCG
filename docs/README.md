# Pokémon TCG — Trabajo Práctico Integrador

---

## Índice

1. [Descripción del Proyecto](#1-descripción-del-proyecto)
2. [Stack Tecnológico](#2-stack-tecnológico)
3. [Guía de Instalación y Ejecución](#3-guía-de-instalación-y-ejecución)

---

## 1. Descripción del Proyecto

### Contexto

El **Pokémon Trading Card Game (TCG)** es un juego de cartas coleccionables en el que dos jugadores se enfrentan con mazos de **60 cartas** construidos estratégicamente. Cada jugador utiliza **Pokémon** para combatir, **Energías** para potenciarlos y **cartas de Entrenador** para obtener ventajas tácticas.

### Objetivo

Desarrollar una **versión digital completamente funcional** del Pokémon TCG implementando tanto el Back End (Java + Spring Boot) como el Front End (Angular), permitiendo a dos jugadores competir en partidas en tiempo real. El juego debe respetar íntegramente el reglamento oficial **XY1 Rulebook** e integrar las cartas reales a través de la API pública `pokemontcg.io (v2)`.

### Condiciones de Victoria

Un jugador puede ganar de tres formas:

| Condición | Descripción |
|---|---|
| **Victoria por Premios** | Tomar su última carta de Premio |
| **Knockout total** | El oponente no tiene Pokémon en campo para reemplazar al derrotado |
| **Mazo vacío del rival** | El oponente intenta robar con el mazo vacío al inicio de su turno |
| **Muerte Súbita** | Si ambas condiciones se cumplen simultáneamente, nueva partida con 1 carta de Premio |

---

## 2. Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| **Backend** | Java + Spring Boot | Java 21+ / Spring Boot 3.x |
| **Frontend** | Angular + TypeScript | Angular 21+ |
| **Base de Datos** | PostgreSQL o MySQL | PostgreSQL 15+ recomendado |
| **Comunicación RT** | WebSockets (STOMP sobre SockJS) | Spring WebSocket |
| **Build Backend** | Maven | 3.9+ |
| **Testing Backend** | JUnit + Mockito + JaCoCo | JUnit 5 |
| **Documentación API** | Swagger / OpenAPI | springdoc-openapi 2.x |
| **API Externa** | pokemontcg.io | v2 |
| **Control de versiones** | Git + GitHub (GitFlow) | — |

### Restricción de Set de Cartas

El set de referencia **obligatorio** es:

```
Set: XY Unlimited
Código API: xy1
Lanzamiento: 05/02/2014
Total de cartas: 146
Endpoint: https://api.pokemontcg.io/v2/cards?q=set.id:xy1
```

## 3. Guía de Instalación y Ejecución

### Prerrequisitos

- Java 21+
- Maven 3.9+
- Node.js 20+ y npm 10+
- PostgreSQL 15+ (o MySQL 8+) en ejecución
- Angular CLI 21+ (`npm install -g @angular/cli`)

### Backend

```bash
# 1. Clonar el repositorio
git clone https://github.com/[org]/pokemon-tcg.git
cd pokemon-tcg

# 2. Configurar la base de datos
# Editar backend/src/main/resources/application.properties:
# spring.datasource.url=jdbc:postgresql://localhost:5432/pokemon_tcg
# spring.datasource.username=tu_usuario
# spring.datasource.password=tu_password

# 3. Crear la base de datos
psql -U postgres -c "CREATE DATABASE pokemon_tcg;"

# 4. Ejecutar migraciones y seed
psql -U postgres -d pokemon_tcg -f sql/schema.sql
psql -U postgres -d pokemon_tcg -f sql/seed-data.sql

# 5. Compilar y ejecutar
cd backend
mvn clean install
mvn spring-boot:run

# Backend disponible en: http://localhost:8080
# Swagger UI en: http://localhost:8080/swagger-ui.html
```

### Frontend

```bash
# 1. Instalar dependencias
cd frontend
npm install

# 2. Ejecutar en modo desarrollo
ng serve

# Frontend disponible en: http://localhost:4200
```

### Sincronizar caché de cartas (primer uso)

```bash
curl -X POST http://localhost:8080/api/cards/sync
# Descarga todas las cartas del set xy1 desde pokemontcg.io y las persiste localmente
```

### Ejecutar Tests

```bash
# Backend — todos los tests
cd backend && mvn test

# Backend — reporte de cobertura JaCoCo
cd backend && mvn verify
# Reporte en: backend/target/site/jacoco/index.html

# Frontend — tests E2E
cd frontend && ng e2e
```
