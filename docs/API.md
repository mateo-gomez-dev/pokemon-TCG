## Índice

1. [API REST — Contratos](#1-api-rest--contratos)
2. [WebSockets — Protocolo de Eventos](#2-websockets--protocolo-de-eventos)

## 1. API REST — Contratos

> La especificación completa está en `docs/openapi.yaml`. Este es un resumen de los endpoints principales.

### Deck Builder

```
GET    /api/cards?setId=xy1&name=...      Buscar cartas (caché local primero)
POST   /api/cards/sync                    Sincronizar caché desde pokemontcg.io

GET    /api/decks                         Listar mazos del jugador autenticado
POST   /api/decks                         Crear mazo
GET    /api/decks/{deckId}               Obtener mazo por ID
PUT    /api/decks/{deckId}               Actualizar mazo
DELETE /api/decks/{deckId}               Eliminar mazo
POST   /api/decks/{deckId}/validate      Validar mazo (retorna errores detallados)
```

### Lobby y Matchmaking

```
GET    /api/lobby                         Listar partidas disponibles (estado WAITING)
POST   /api/games                         Crear nueva partida (jugador 1)
POST   /api/games/{gameId}/join           Unirse a partida (jugador 2)
GET    /api/games/{gameId}               Obtener estado de partida
```

### Acciones de Juego

```
POST   /api/games/{gameId}/actions/draw              Robar carta
POST   /api/games/{gameId}/actions/play-card         Jugar carta de la mano
POST   /api/games/{gameId}/actions/attach-energy     Unir Energía a Pokémon
POST   /api/games/{gameId}/actions/evolve            Evolucionar Pokémon
POST   /api/games/{gameId}/actions/use-ability       Usar Habilidad
POST   /api/games/{gameId}/actions/retreat           Retirar Pokémon Activo
POST   /api/games/{gameId}/actions/attack            Declarar ataque
POST   /api/games/{gameId}/actions/end-turn          Finalizar turno
GET    /api/games/{gameId}/log                       Obtener log de acciones
```

---

## 2. WebSockets — Protocolo de Eventos

### Conexión

```
URL de conexión: ws://localhost:8080/ws
Protocolo: STOMP sobre SockJS
```

### Suscripciones del cliente

```
/topic/game/{gameId}          ← Eventos de la partida (ambos jugadores)
/user/queue/game/{gameId}     ← Eventos privados (solo para este jugador)
/topic/lobby                  ← Actualizaciones del lobby
```

### Tipos de Eventos Emitidos por el Servidor

```json
// Estado actualizado tras cada acción
{ "type": "GAME_STATE_UPDATE", "payload": { ...GameStateResponse } }

// Inicio de turno
{ "type": "TURN_START", "payload": { "currentPlayerId": "...", "turnNumber": 5 } }

// Knockout
{ "type": "POKEMON_KO", "payload": { "pokemonName": "Charizard-EX", "position": "ACTIVE", "ownerId": "..." } }

// Toma de Premio
{ "type": "PRIZE_TAKEN", "payload": { "playerId": "...", "prizesRemaining": 3 } }

// Condición especial aplicada
{ "type": "SPECIAL_CONDITION", "payload": { "pokemonId": "...", "condition": "POISONED" } }

// Fin de partida
{ "type": "GAME_OVER", "payload": { "winnerId": "...", "reason": "PRIZES" } }

// Muerte Súbita
{ "type": "SUDDEN_DEATH", "payload": { "message": "Nueva partida con 1 Premio" } }

// Reconexión
{ "type": "RECONNECT_STATE", "payload": { ...GameStateResponse } }
```

### Acciones Enviadas por el Cliente (vía STOMP)

```
/app/game/{gameId}/action      ← Enviar acción de juego
/app/lobby/join                ← Unirse al lobby
```

---