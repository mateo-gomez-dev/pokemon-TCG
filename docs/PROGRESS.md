# PROGRESS.md — Pokémon TCG

## 📋 Convenciones
- ✅ Completado
- 🔄 En Proceso
- ⏳ Pendiente
- 🚫 Bloqueado (depende de otra task)

---

## ETAPA 0 — Diseño y Documentación

| Task | Estado | .md de referencia |
|---|---|---|
| Definir arquitectura general | ✅ | ARCHITECTURE.md |
| Definir modelo de dominio | ✅ | DOMAIN.md |
| Definir esquema de base de datos | ✅ | DATABASE.md |
| Definir contratos API REST | ✅ | API.md |
| Definir protocolo WebSocket | ✅ | API.md |
| Definir estrategia de testing | ✅ | TESTING.md |
| Definir requerimientos funcionales y no funcionales | ✅ | REQUIREMENTS.md |
| Definir features extras | ✅ | EXTRAS.md |

---

## ETAPA 1 — Setup del Proyecto

| Task | Estado | .md de referencia |
|---|---|---|
| Inicializar proyecto Spring Boot (Java 21, Maven) | ✅ | README.md |
| Inicializar proyecto Angular 21+ | ✅ | README.md |
| Configurar base de datos PostgreSQL | ✅ | DATABASE.md |
| Ejecutar schema.sql (DDL completo) | ✅ | DATABASE.md |
| Ejecutar seed-data.sql (mazo de ejemplo xy1) | ✅ | DATABASE.md |
| Configurar Swagger / OpenAPI (springdoc-openapi 2.x) | ✅ | README.md |
| Configurar JaCoCo para cobertura | ✅ | REQUIREMENTS.md |
| Configurar estructura de carpetas del backend | ✅ | ARCHITECTURE.md |
| Configurar estructura de carpetas del frontend | ✅ | ARCHITECTURE.md |

---

## ETAPA 2 — Dominio y Game Engine (TDD)

> Orden de desarrollo según TDD recomendado en ARCHITECTURE.md.
> Contexto a adjuntar: **ARCHITECTURE.md + DOMAIN.md + TESTING.md + PROGRESS.md**

### 2.1 — Modelos de Dominio

| Task | Estado | Notas |
|---|---|---|
| Card (base) + PokemonCard + EnergyCard + TrainerCard | ✅ | Jerarquía de herencia |
| Value Objects: HP, DamageCounter, SpecialCondition, EnergyAttachment, PrizeCards | ✅ | Inmutables |
| Deck + Hand + Board + ActivePokemon + BenchPokemon | ✅ | |
| Game + Player + Turn + TurnPhase (enum) | ✅ | |
| TurnState | ✅ | Flags: energíaUsada, retiradaUsada, partidarioUsado |

### 2.2 — Puertos (Interfaces Hexagonales)

| Task | Estado | Notas |
|---|---|---|
| GameEnginePort (puerto primario) | ✅ | |
| GameRepository (puerto secundario) | ✅ | |
| CardCacheRepository (puerto secundario) | ✅ | |
| DeckRepository (puerto secundario) | ✅ | |
| EventPublisher (puerto secundario) | ✅ | |
| RandomProvider (puerto secundario) | ✅ | Para moneda testeable |

### 2.3 — Game Engine (con TDD)

| Task | Estado | Notas |
|---|---|---|
| DamageCalculator + DamageCalculatorTest | ⏳ | Debilidad ×2, Resistencia −20 |
| StatusEffectManager + StatusEffectManagerTest | ⏳ | Orden fijo, exclusividad |
| RuleValidator + RuleValidatorTest | ⏳ | Cobertura ≥ 90% |
| VictoryConditionChecker + VictoryConditionCheckerTest | ⏳ | 3 condiciones + Muerte Súbita |
| TurnManager + TurnManagerTest | ⏳ | Fases DRAW/MAIN/ATTACK/BETWEEN_TURNS |
| AttackPipeline (Chain of Responsibility) + AttackPipelineTest | ⏳ | 7 pasos |
| — EnergyValidationStep | ⏳ | Paso 1 |
| — ConfusionCheckStep | ⏳ | Paso 2 |
| — SelectionStep | ⏳ | Paso 3 |
| — PreAttackStep | ⏳ | Paso 4 |
| — ModifierStep | ⏳ | Paso 5 |
| — DamageApplicationStep | ⏳ | Paso 6 |
| — PostDamageEffectStep | ⏳ | Paso 7 |
| GameEngineFacade (Facade Pattern) + GameEngineTest | ⏳ | Integra todo el engine |

### 2.4 — State Pattern (Estados de la Partida)

| Task | Estado | Notas |
|---|---|---|
| GameState (interfaz) | ⏳ | |
| WaitingState | ⏳ | |
| SetupState | ⏳ | Mulligan, colocación inicial |
| ActiveState | ⏳ | Delega al TurnManager |
| FinishedState | ⏳ | Incluye Muerte Súbita |

### 2.5 — Strategy Pattern (Efectos de Cartas)

| Task | Estado | Notas |
|---|---|---|
| TrainerEffect (interfaz) | ⏳ | |
| ItemEffect | ⏳ | |
| SupporterEffect | ⏳ | |
| StadiumEffect | ⏳ | |
| AttackEffect | ⏳ | |

---

## ETAPA 3 — Tests de Integración del Engine

> Contexto: **TESTING.md + ARCHITECTURE.md + PROGRESS.md**

| Task | Estado | Notas |
|---|---|---|
| FullGameIntegrationTest | ⏳ | Setup → turnos → victoria por premios |
| MulliganIntegrationTest | ⏳ | Mulligan múltiple, carta extra al rival |
| EvolutionIntegrationTest | ⏳ | Restricciones de evolución |
| KnockoutIntegrationTest | ⏳ | KO + reemplazo obligatorio + 2 premios EX |
| VictoryIntegrationTest | ⏳ | Las 3 condiciones de victoria |
| Verificar cobertura JaCoCo ≥ 80% global | ⏳ | |
| Verificar cobertura RuleValidator ≥ 90% | ⏳ | |
| Verificar cobertura DamageCalculator ≥ 90% | ⏳ | |
| Verificar cobertura StatusEffectManager ≥ 90% | ⏳ | |

---

## ETAPA 4 — Capa de Aplicación (Use Cases)

> Contexto: **ARCHITECTURE.md + API.md + PROGRESS.md**

| Task | Estado | Notas |
|---|---|---|
| DTOs de comando: PlayCardCommand, AttachEnergyCommand, AttackCommand, RetreatCommand | ⏳ | |
| DTOs de respuesta: GameStateResponse, PlayerViewResponse, CardResponse | ⏳ | Nunca exponer mano rival |
| CardCacheService (integración pokemontcg.io) | ⏳ | Sync set xy1 |
| DeckService (CRUD mazos + validación) | ⏳ | |
| MatchmakingService | ⏳ | |
| GameService | ⏳ | |
| ReconnectionService | ⏳ | |
| GameServiceTest (Mockito) | ⏳ | |
| DeckServiceTest (Mockito) | ⏳ | |

---

## ETAPA 5 — Infraestructura / Persistencia

> Contexto: **ARCHITECTURE.md + DATABASE.md + PROGRESS.md**

| Task | Estado | Notas |
|---|---|---|
| Entidades JPA: GameEntity, PlayerEntity, DeckEntity, CardEntity, GameLogEntity | ⏳ | |
| SpringGameRepository | ⏳ | |
| SpringDeckRepository | ⏳ | |
| SpringCardRepository | ⏳ | |
| GameRepositoryAdapter (implementa puerto) | ⏳ | |
| DeckRepositoryAdapter (implementa puerto) | ⏳ | |
| CardCacheRepositoryAdapter (implementa puerto) | ⏳ | |
| PokemonTcgApiAdapter (cliente HTTP pokemontcg.io) | ⏳ | |
| Persistencia de game_state_json en JSONB | ⏳ | Snapshot completo tras cada acción |
| Persistencia inmutable de game_logs | ⏳ | |
| Persistencia de prize_cards (ocultas) | ⏳ | |

---

## ETAPA 6 — API REST

> Contexto: **API.md + ARCHITECTURE.md + PROGRESS.md**

| Task | Estado | Notas |
|---|---|---|
| CardController (GET /api/cards, POST /api/cards/sync) | ⏳ | |
| DeckController (CRUD + validate) | ⏳ | |
| LobbyController (GET /api/lobby) | ⏳ | |
| GameController (crear, unirse, obtener estado) | ⏳ | |
| Game Actions endpoints (draw, play-card, attach-energy, evolve, use-ability, retreat, attack, end-turn) | ⏳ | |
| GET /api/games/{gameId}/log | ⏳ | |
| Manejo de errores HTTP (400, 401, 403, 404, 409, 500) | ⏳ | |
| Validación de entradas en backend | ⏳ | |

---

## ETAPA 7 — WebSockets

> Contexto: **API.md + ARCHITECTURE.md + PROGRESS.md**

| Task | Estado | Notas |
|---|---|---|
| WebSocketConfig (STOMP sobre SockJS) | ⏳ | ws://localhost:8080/ws |
| GameWebSocketHandler | ⏳ | |
| WebSocketEventPublisher (implementa EventPublisher) | ⏳ | Observer Pattern |
| Eventos: GAME_STATE_UPDATE, TURN_START, POKEMON_KO, PRIZE_TAKEN | ⏳ | |
| Eventos: SPECIAL_CONDITION, GAME_OVER, SUDDEN_DEATH, RECONNECT_STATE | ⏳ | |
| Reconexión robusta (estado actual al reconectarse) | ⏳ | |

---

## ETAPA 8 — Frontend Angular

> Contexto: **API.md + DOMAIN.md + PROGRESS.md**

### 8.1 — Core

| Task | Estado | Notas |
|---|---|---|
| Modelos TypeScript: game-state, card, player-view, websocket-event | ⏳ | Contrato con backend |
| game.service.ts | ⏳ | |
| deck.service.ts | ⏳ | |
| websocket.service.ts | ⏳ | Reconexión automática |
| auth.service.ts | ⏳ | |

### 8.2 — Features

| Task | Estado | Notas |
|---|---|---|
| Lobby (crear / unirse a partida) | ⏳ | |
| Deck Builder (buscar, agregar, validar, CRUD) | ⏳ | Validaciones en tiempo real |
| Game Board — Board component | ⏳ | |
| Game Board — ActivePokemon component | ⏳ | HP, condición especial, energías |
| Game Board — Bench component | ⏳ | Hasta 5 Pokémon |
| Game Board — Hand component | ⏳ | Solo mano propia |
| Game Board — PrizeCards component | ⏳ | Ocultas hasta revelar |
| Game Board — ActionPanel component | ⏳ | Botones habilitados/deshabilitados por fase |
| Game Board — GameLog component | ⏳ | Historial cronológico |
| Drag & Drop (Básico a Banca, Energía, Herramienta, Entrenador) | ⏳ | |
| Highlight de targets válidos | ⏳ | |
| Notificaciones visuales de eventos | ⏳ | |

### 8.3 — Tests E2E

| Task | Estado | Notas |
|---|---|---|
| Crear mazo → unirse a partida → ejecutar un turno | ⏳ | |

---

## ETAPA 9 — Requerimientos Opcionales del Enunciado

> Contexto: **REQUIREMENTS.md + PROGRESS.md**

| Task | Estado | Notas |
|---|---|---|
| Chat entre jugadores (WebSocket) | ⏳ | |
| Sistema de ranking / historial de partidas | ⏳ | |
| Animaciones (ataques, evoluciones, knockouts) | ⏳ | Solo frontend |
| Pokémon Megaevolución | ⏳ | Turno termina al jugarla, KO = 2 premios |

---

## ETAPA 10 — Extras (EXTRAS.md)

> Solo iniciar una vez que ETAPA 0–9 estén completas y testeadas.

| Task | Estado | Prioridad | Notas |
|---|---|---|---|
| F-04 Modo Usuario vs IA (básico) | ⏳ | Muy Alta | Usa GameEngineFacade, no reimplementa reglas |
| F-05 Sistema de Dificultad de IA (Fácil/Medio/Difícil) | ⏳ | Alta | Strategy Pattern |
| F-06 Modo Automático (Auto-Play / IA vs IA) | ⏳ | Alta | Reutiliza F-04 |
| F-01 Requerimientos opcionales (si no se hicieron en E9) | ⏳ | Alta | |
| F-07 Sistema MMR / Partidas Competitivas | ⏳ | Media | |
| F-08 Sistema de Amigos | ⏳ | Media | |
| F-02 Expansión de Cartas (múltiples sets) | ⏳ | Media | Engine independiente del set |
| F-09 Favoritos y Filtros en Deck Builder | ⏳ | Baja | Solo frontend |
| F-03 Sistema de Música Dinámica | ⏳ | Baja | Solo frontend |

---

## 📊 Resumen General

| Etapa | Estado |
|---|---|
| 0 — Diseño y Documentación | ✅ |
| 1 — Setup del Proyecto | ✅ |
| 2 — Dominio y Game Engine | ⏳ |
| 3 — Tests de Integración | ⏳ |
| 4 — Capa de Aplicación | ⏳ |
| 5 — Infraestructura / Persistencia | ⏳ |
| 6 — API REST | ⏳ |
| 7 — WebSockets | ⏳ |
| 8 — Frontend Angular | ⏳ |
| 9 — Opcionales del Enunciado | ⏳ |
| 10 — Extras | ⏳ |
