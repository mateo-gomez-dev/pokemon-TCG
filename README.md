# Pokemon TCG - TPI Academico

Proyecto nuevo desde cero para un TPI academico de Pokemon TCG.

## Backend

El backend base esta en la carpeta `BE/` y usa Java 21, Spring Boot 3.x y Maven.

### Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL en ejecucion
- Base de datos creada: `pokemon_tcg`

### Configurar Base De Datos

Editar `BE/src/main/resources/application.properties` y reemplazar el placeholder:

```properties
spring.datasource.password=CAMBIAR_PASSWORD
```

por la password real del usuario `postgres`.

La configuracion actual usa:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pokemon_tcg
spring.datasource.username=postgres
server.port=8080
spring.jpa.hibernate.ddl-auto=update
```

### Ejecutar Backend

```bash
cd BE
mvn spring-boot:run
```

### Probar Ping

```bash
curl http://localhost:8080/ping
```

Respuesta esperada:

```text
pong
```

### Swagger

Con el backend en ejecucion, abrir:

```text
http://localhost:8080/swagger-ui.html
```

### Importar Cartas XY1

El backend usa `pokemontcg.io` solo para cargar el cache local de cartas. Durante una partida no se debe llamar a la API externa.

Para importar o actualizar las 146 cartas del set XY (`xy1`) en PostgreSQL:

```bash
curl -X POST http://localhost:8080/api/cards/import/xy1
```

La importacion usa este origen:

```text
https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250
```

No se descargan imagenes fisicamente. Solo se guardan las URLs `images.small` e `images.large`.

Para listar las cartas cacheadas:

```bash
curl http://localhost:8080/api/cards
```

Para consultar una carta por id:

```bash
curl http://localhost:8080/api/cards/xy1-1
```

### Deck Builder Backend

Los mazos usan las cartas ya importadas en la tabla `cards`. Primero importar `xy1` antes de crear mazos.

Reglas de validacion implementadas:

- exactamente 60 cartas
- maximo 4 copias del mismo nombre, excepto Energia Basica
- maximo 1 carta de AS TACTICO / ACE SPEC
- al menos 1 Pokemon Basico

Crear mazo:

```bash
curl -X POST http://localhost:8080/api/decks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mazo ejemplo",
    "cards": [
      { "cardId": "xy1-1", "quantity": 4 },
      { "cardId": "xy1-2", "quantity": 4 },
      { "cardId": "xy1-3", "quantity": 52 }
    ]
  }'
```

Listar mazos:

```bash
curl http://localhost:8080/api/decks
```

Consultar mazo por id:

```bash
curl http://localhost:8080/api/decks/1
```

Editar mazo:

```bash
curl -X PUT http://localhost:8080/api/decks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mazo editado",
    "cards": [
      { "cardId": "xy1-1", "quantity": 4 },
      { "cardId": "xy1-2", "quantity": 4 },
      { "cardId": "xy1-3", "quantity": 52 }
    ]
  }'
```

Validar mazo:

```bash
curl -X POST http://localhost:8080/api/decks/1/validate
```

Eliminar mazo:

```bash
curl -X DELETE http://localhost:8080/api/decks/1
```

### Partidas Backend Basico

Esta etapa agrega estructura, persistencia basica de partidas, union de segundo jugador, inicio basico y acciones basicas de turno. Todavia no hay ataques, daño, knockout, condiciones especiales, WebSockets ni frontend de partida.

Crear partida en estado `WAITING` con un jugador inicial y un mazo existente:

```bash
curl -X POST http://localhost:8080/api/games \
  -H "Content-Type: application/json" \
  -d '{
    "playerName": "Ash",
    "deckId": 1
  }'
```

Unirse como segundo jugador a una partida `WAITING`:

```bash
curl -X POST http://localhost:8080/api/games/1/join \
  -H "Content-Type: application/json" \
  -d '{
    "playerName": "Jugador 2",
    "deckId": 1
  }'
```

Iniciar una partida `WAITING` con 2 jugadores y mazos validos:

```bash
curl -X POST http://localhost:8080/api/games/1/start
```

Al iniciar, el backend valida ambos mazos, baraja, reparte 7 cartas a la mano, separa 6 premios, deja el resto como deck restante, define el jugador actual como jugador 1 y la fase inicial como `DRAW`.

Robar carta con el jugador actual:

```bash
curl -X POST http://localhost:8080/api/games/1/actions/draw \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": 1
  }'
```

Al robar, el backend mueve 1 carta de `deckCardIds` a `handCardIds`, registra `DRAW_CARD` y pasa la fase a `MAIN`.

Finalizar turno con el jugador actual:

```bash
curl -X POST http://localhost:8080/api/games/1/actions/end-turn \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": 1
  }'
```

Al finalizar turno, el backend cambia `currentPlayerId` al otro jugador, registra `END_TURN` y vuelve la fase a `DRAW`.

Jugar un Pokemon Basico desde la mano a la banca:

```bash
curl -X POST http://localhost:8080/api/games/1/actions/play-basic-pokemon \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": 1,
    "cardId": "xy1-1"
  }'
```

Esta accion solo funciona en fase `MAIN`, requiere que la carta este en la mano, que exista en `cards`, que sea `supertype = Pokémon`, que tenga subtype `Basic` y que la banca tenga menos de 5 Pokemon. El backend mueve la carta de `handCardIds` a `benchCardIds` y registra `PLAY_BASIC_POKEMON`.

Unir una Energia Basica desde la mano a un Pokemon en banca:

```bash
curl -X POST http://localhost:8080/api/games/1/actions/attach-energy \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": 1,
    "energyCardId": "xy1-132",
    "targetPokemonCardId": "xy1-1"
  }'
```

Esta accion solo funciona en fase `MAIN`, requiere que la Energia este en la mano, que exista en `cards`, que sea `supertype = Energy`, que tenga subtype `Basic`, que el Pokemon objetivo este en `benchCardIds` y que el jugador no haya unido Energia este turno. El backend mueve la Energia de `handCardIds` a `attachedEnergyCardIdsByPokemonCardId`, marca `energyAttachedThisTurn = true` y registra `ATTACH_ENERGY`.

Listar partidas:

```bash
curl http://localhost:8080/api/games
```

Consultar partida por id:

```bash
curl http://localhost:8080/api/games/1
```

### Tests

```bash
cd BE
mvn test
```

## Frontend

El frontend esta en la carpeta `FE/` y usa Angular con TypeScript, routing y servicios HTTP.

Pantallas disponibles:

- Home: `/`
- Cartas: `/cards`
- Mazos: `/decks`
- Deck Builder: `/deck-builder`
- Partidas: `/game`

El frontend usa los endpoints reales del backend:

- `GET /api/cards`
- `GET /api/decks`
- `GET /api/decks/{id}`
- `POST /api/decks`
- `PUT /api/decks/{id}`
- `DELETE /api/decks/{id}`
- `POST /api/decks/{id}/validate`
- `GET /api/games`
- `GET /api/games/{id}`
- `POST /api/games`
- `POST /api/games/{id}/join`
- `POST /api/games/{id}/start`
- `POST /api/games/{id}/actions/draw`
- `POST /api/games/{id}/actions/end-turn`
- `POST /api/games/{id}/actions/play-basic-pokemon`
- `POST /api/games/{id}/actions/attach-energy`

No hay endpoints `/api/tarjetas` implementados actualmente. Conviene mantener unificado el backend en `/api/cards`.

### Ejecutar Frontend

Primero levantar el backend en `http://localhost:8080`.

Luego instalar dependencias y ejecutar Angular:

```bash
cd FE
npm install
npm start
```

Abrir:

```text
http://localhost:4200
```

El comando `npm start` usa `proxy.conf.json` para reenviar `/api` y `/ping` al backend, evitando problemas de CORS en desarrollo.

### Flujo Rapido De Prueba UI

1. Levantar PostgreSQL y backend.
2. Importar cartas XY1 si la tabla esta vacia: `POST /api/cards/import/xy1`.
3. Abrir `http://localhost:4200/decks` y usar `Mazo prueba` para crear mazos validos de 60 cartas.
4. Abrir `http://localhost:4200/game`, crear una partida con un mazo valido y seleccionar la partida creada.
5. Unir el segundo jugador con otro mazo valido, iniciar la partida y usar las acciones de robar, jugar Pokemon Basico, unir Energia y finalizar turno.

### Build Frontend

```bash
cd FE
npm run build
```
