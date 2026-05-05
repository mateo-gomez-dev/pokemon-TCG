## Índice

1. [Filosofía de Arquitectura](#1-filosofía-de-arquitectura)
2. [Estructura del Proyecto](#2-estructura-del-proyecto)
3. [Patrones de Diseño Aplicados](#3-patrones-de-diseño-aplicados)

## 1. Filosofía de Arquitectura

Este proyecto adopta una combinación de cuatro enfoques complementarios que deben aplicarse de forma consistente en todo el código:

### 1.1 Clean Architecture

Cada módulo del sistema se organiza en capas concéntricas donde **las dependencias solo apuntan hacia adentro**:

```
┌─────────────────────────────────────────────────────────────┐
│                    FRAMEWORKS & DRIVERS                      │
│         (Spring Boot, Angular, WebSocket, JPA, etc.)        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              INTERFACE ADAPTERS                        │  │
│  │   (Controllers REST, WebSocket Handlers, Presenters,  │  │
│  │    Repositories Impl, Mappers DTO ↔ Domain)           │  │
│  │  ┌─────────────────────────────────────────────────┐  │  │
│  │  │            APPLICATION / USE CASES              │  │  │
│  │  │   (GameService, DeckService, MatchmakingService, │  │  │
│  │  │    TurnService, AttackService, etc.)             │  │  │
│  │  │  ┌───────────────────────────────────────────┐  │  │  │
│  │  │  │             DOMAIN / CORE                 │  │  │  │
│  │  │  │  Entities: Game, Player, Pokemon, Card,   │  │  │  │
│  │  │  │  Deck, Turn, Attack, Energy               │  │  │  │
│  │  │  │  Value Objects: HP, DamageCounter,        │  │  │  │
│  │  │  │  EnergyAttachment, SpecialCondition       │  │  │  │
│  │  │  │  Domain Services: RuleValidator,          │  │  │  │
│  │  │  │  DamageCalculator, StatusEffectManager,   │  │  │  │
│  │  │  │  VictoryConditionChecker, TurnManager     │  │  │  │
│  │  │  │  Ports (interfaces): GameRepository,      │  │  │  │
│  │  │  │  CardRepository, DeckRepository,          │  │  │  │
│  │  │  │  EventPublisher                           │  │  │  │
│  │  │  └───────────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**Regla fundamental:** El dominio (core) no importa ninguna clase de Spring, JPA, Angular ni ningún framework externo. Es puro Java/TypeScript.

### 1.2 Arquitectura Hexagonal (Puertos y Adaptadores)

El **Game Engine** es el hexágono central. Se comunica con el mundo exterior exclusivamente a través de interfaces (puertos):

```
                    ┌──────────────────┐
      REST API ────▶│   PRIMARY PORT   │
   WebSocket ──────▶│  (Driving Side)  │
                    │                  │
                    │   GAME ENGINE    │
                    │   (Hexágono)     │
                    │                  │
                    │  SECONDARY PORT  │◀──── Database (JPA Adapter)
                    │  (Driven Side)   │◀──── pokemontcg.io (HTTP Adapter)
                    │                  │◀──── EventBus (WS Adapter)
                    └──────────────────┘
```

**Puertos primarios (Driving):** Interfaces que el mundo exterior usa para llamar al Engine.

```java
// Puerto primario — cualquier adaptador (REST, WS, Test) puede implementar esto
public interface GameEnginePort {
    GameState setupGame(String gameId, PlayerDeck deckA, PlayerDeck deckB);
    GameState drawCard(String gameId, String playerId);
    GameState playCard(String gameId, String playerId, PlayCardCommand command);
    GameState attachEnergy(String gameId, String playerId, AttachEnergyCommand command);
    GameState declareAttack(String gameId, String playerId, AttackCommand command);
    GameState retreat(String gameId, String playerId, RetreatCommand command);
    GameState endTurn(String gameId, String playerId);
    GameState processBetweenTurns(String gameId);
    GameState reconnect(String gameId, String playerId);
}
```

**Puertos secundarios (Driven):** Interfaces que el Engine necesita del mundo exterior.

```java
public interface GameRepository { ... }       // Adaptador: JPA / PostgreSQL
public interface CardCacheRepository { ... }  // Adaptador: Base de datos local (caché de pokemontcg.io)
public interface EventPublisher { ... }        // Adaptador: WebSocket STOMP
public interface RandomProvider { ... }        // Adaptador: java.util.Random (testeable)
```
### 1.3 TDD — Test-Driven Development

Todo el **Game Engine** (dominio + casos de uso) se desarrolla siguiendo el ciclo estricto:

```
RED  ──▶  GREEN  ──▶  REFACTOR
 │                        │
 └──── nuevo ciclo ◀──────┘
```

**Orden recomendado de desarrollo TDD:**

1. `DamageCalculatorTest` → implementar `DamageCalculator`
2. `StatusEffectManagerTest` → implementar `StatusEffectManager`
3. `RuleValidatorTest` → implementar `RuleValidator`
4. `VictoryConditionCheckerTest` → implementar `VictoryConditionChecker`
5. `TurnManagerTest` → implementar `TurnManager`
6. `AttackPipelineTest` → implementar `AttackPipeline` (Chain of Responsibility)
7. `GameEngineTest` → implementar `GameEngineFacade`

**Cobertura mínima requerida:**

| Componente | Cobertura Mínima |
|---|---|
| `RuleValidator` | ≥ 90% |
| `DamageCalculator` | ≥ 90% |
| `StatusEffectManager` | ≥ 90% |
| Resto del proyecto | ≥ 80% |

### 1.4 SDD — Specification-Driven Development

El diseño comienza con una **especificación formal** antes de escribir código. Esto implica:

1. **Definir los contratos de API** (OpenAPI/Swagger) antes de implementar los endpoints.
2. **Definir el esquema de base de datos** antes de crear las entidades JPA.
3. **Definir los eventos WebSocket** antes de implementar los handlers.
4. **Definir los casos de prueba** (especificaciones) antes de implementar la lógica.

El archivo `openapi.yaml` y el script `schema.sql` son documentos vivos que se actualizan primero y sirven como contratos entre Back End y Front End.

## 2. Estructura del Proyecto

```
pokemon-tcg/
├── backend/                          ← Módulo Spring Boot
│   ├── src/
│   │   ├── main/java/com/pokemon/tcg/
│   │   │   │
│   │   │   ├── domain/               ← CAPA DE DOMINIO (sin dependencias externas)
│   │   │   │   ├── model/            ← Entidades de dominio y Value Objects
│   │   │   │   │   ├── Game.java
│   │   │   │   │   ├── Player.java
│   │   │   │   │   ├── Card.java
│   │   │   │   │   ├── PokemonCard.java
│   │   │   │   │   ├── EnergyCard.java
│   │   │   │   │   ├── TrainerCard.java
│   │   │   │   │   ├── Deck.java
│   │   │   │   │   ├── Hand.java
│   │   │   │   │   ├── Board.java
│   │   │   │   │   ├── ActivePokemon.java
│   │   │   │   │   ├── BenchPokemon.java
│   │   │   │   │   ├── Turn.java
│   │   │   │   │   └── vo/           ← Value Objects
│   │   │   │   │       ├── HP.java
│   │   │   │   │       ├── DamageCounter.java
│   │   │   │   │       ├── SpecialCondition.java
│   │   │   │   │       ├── EnergyAttachment.java
│   │   │   │   │       └── PrizeCards.java
│   │   │   │   │
│   │   │   │   ├── engine/           ← Game Engine (núcleo de reglas)
│   │   │   │   │   ├── GameEngineFacade.java       ← Patrón Facade
│   │   │   │   │   ├── RuleValidator.java
│   │   │   │   │   ├── DamageCalculator.java
│   │   │   │   │   ├── StatusEffectManager.java
│   │   │   │   │   ├── VictoryConditionChecker.java
│   │   │   │   │   ├── TurnManager.java
│   │   │   │   │   └── pipeline/     ← Patrón Chain of Responsibility
│   │   │   │   │       ├── AttackPipeline.java
│   │   │   │   │       ├── AttackStep.java          ← interfaz del paso
│   │   │   │   │       ├── EnergyValidationStep.java
│   │   │   │   │       ├── ConfusionCheckStep.java
│   │   │   │   │       ├── SelectionStep.java
│   │   │   │   │       ├── PreAttackStep.java
│   │   │   │   │       ├── ModifierStep.java
│   │   │   │   │       ├── DamageApplicationStep.java
│   │   │   │   │       └── PostDamageEffectStep.java
│   │   │   │   │
│   │   │   │   ├── state/            ← Patrón State (estados del juego y del turno)
│   │   │   │   │   ├── GameState.java              ← interfaz estado de partida
│   │   │   │   │   ├── WaitingState.java
│   │   │   │   │   ├── SetupState.java
│   │   │   │   │   ├── ActiveState.java
│   │   │   │   │   ├── FinishedState.java
│   │   │   │   │   ├── TurnPhase.java              ← enum: DRAW, MAIN, ATTACK, BETWEEN_TURNS
│   │   │   │   │   └── TurnState.java
│   │   │   │   │
│   │   │   │   ├── strategy/         ← Patrón Strategy (efectos de cartas)
│   │   │   │   │   ├── TrainerEffect.java           ← interfaz
│   │   │   │   │   ├── ItemEffect.java
│   │   │   │   │   ├── SupporterEffect.java
│   │   │   │   │   ├── StadiumEffect.java
│   │   │   │   │   └── AttackEffect.java
│   │   │   │   │
│   │   │   │   └── port/             ← Puertos (interfaces hexagonales)
│   │   │   │       ├── in/           ← Puertos primarios (driving)
│   │   │   │       │   └── GameEnginePort.java
│   │   │   │       └── out/          ← Puertos secundarios (driven)
│   │   │   │           ├── GameRepository.java
│   │   │   │           ├── CardCacheRepository.java
│   │   │   │           ├── DeckRepository.java
│   │   │   │           ├── EventPublisher.java
│   │   │   │           └── RandomProvider.java
│   │   │   │
│   │   │   ├── application/          ← CAPA DE APLICACIÓN (casos de uso)
│   │   │   │   ├── service/
│   │   │   │   │   ├── GameService.java
│   │   │   │   │   ├── DeckService.java
│   │   │   │   │   ├── MatchmakingService.java
│   │   │   │   │   ├── CardCacheService.java        ← integración pokemontcg.io
│   │   │   │   │   └── ReconnectionService.java
│   │   │   │   └── dto/
│   │   │   │       ├── command/      ← Objetos de comando (input)
│   │   │   │       │   ├── PlayCardCommand.java
│   │   │   │       │   ├── AttachEnergyCommand.java
│   │   │   │       │   ├── AttackCommand.java
│   │   │   │       │   └── RetreatCommand.java
│   │   │   │       └── response/     ← Objetos de respuesta (output)
│   │   │   │           ├── GameStateResponse.java
│   │   │   │           ├── PlayerViewResponse.java   ← vista parcial (sin mano rival)
│   │   │   │           └── CardResponse.java
│   │   │   │
│   │   │   └── infrastructure/       ← CAPA DE INFRAESTRUCTURA (adaptadores)
│   │   │       ├── adapter/
│   │   │       │   ├── persistence/  ← Adaptadores de BD (implementan puertos out)
│   │   │       │   │   ├── GameRepositoryAdapter.java
│   │   │       │   │   ├── DeckRepositoryAdapter.java
│   │   │       │   │   └── CardCacheRepositoryAdapter.java
│   │   │       │   ├── external/     ← Adaptador pokemontcg.io
│   │   │       │   │   └── PokemonTcgApiAdapter.java
│   │   │       │   └── event/        ← Adaptador WebSocket
│   │   │       │       └── WebSocketEventPublisher.java
│   │   │       ├── persistence/      ← Entidades JPA y Spring Data Repositories
│   │   │       │   ├── entity/
│   │   │       │   │   ├── GameEntity.java
│   │   │       │   │   ├── PlayerEntity.java
│   │   │       │   │   ├── DeckEntity.java
│   │   │       │   │   ├── CardEntity.java
│   │   │       │   │   └── GameLogEntity.java
│   │   │       │   └── repository/
│   │   │       │       ├── SpringGameRepository.java
│   │   │       │       ├── SpringDeckRepository.java
│   │   │       │       └── SpringCardRepository.java
│   │   │       └── web/              ← Adaptadores REST y WebSocket
│   │   │           ├── rest/
│   │   │           │   ├── GameController.java
│   │   │           │   ├── DeckController.java
│   │   │           │   ├── CardController.java
│   │   │           │   └── LobbyController.java
│   │   │           └── websocket/
│   │   │               ├── GameWebSocketHandler.java
│   │   │               └── WebSocketConfig.java
│   │   │
│   │   └── test/java/com/pokemon/tcg/
│   │       ├── domain/engine/        ← Tests TDD del Game Engine
│   │       │   ├── DamageCalculatorTest.java
│   │       │   ├── StatusEffectManagerTest.java
│   │       │   ├── RuleValidatorTest.java
│   │       │   ├── VictoryConditionCheckerTest.java
│   │       │   ├── TurnManagerTest.java
│   │       │   └── pipeline/
│   │       │       └── AttackPipelineTest.java
│   │       ├── application/          ← Tests de casos de uso (Mockito)
│   │       │   ├── GameServiceTest.java
│   │       │   └── DeckServiceTest.java
│   │       └── integration/          ← Tests de integración
│   │           ├── FullGameIntegrationTest.java
│   │           ├── MulliganIntegrationTest.java
│   │           ├── EvolutionIntegrationTest.java
│   │           ├── KnockoutIntegrationTest.java
│   │           └── VictoryIntegrationTest.java
│   │
│   └── pom.xml
│
├── frontend/                         ← Módulo Angular
│   ├── src/app/
│   │   ├── core/                     ← Módulo Core (singleton services, guards)
│   │   │   ├── services/
│   │   │   │   ├── game.service.ts
│   │   │   │   ├── deck.service.ts
│   │   │   │   ├── websocket.service.ts
│   │   │   │   └── auth.service.ts
│   │   │   └── models/               ← Interfaces TypeScript (contrato con backend)
│   │   │       ├── game-state.model.ts
│   │   │       ├── card.model.ts
│   │   │       ├── player-view.model.ts
│   │   │       └── websocket-event.model.ts
│   │   │
│   │   ├── features/                 ← Módulos de funcionalidad
│   │   │   ├── lobby/                ← Pantalla de lobby
│   │   │   ├── deck-builder/         ← Constructor de mazos
│   │   │   └── game-board/           ← Tablero de juego
│   │   │       ├── components/
│   │   │       │   ├── board/
│   │   │       │   ├── active-pokemon/
│   │   │       │   ├── bench/
│   │   │       │   ├── hand/
│   │   │       │   ├── prize-cards/
│   │   │       │   ├── action-panel/
│   │   │       │   └── game-log/
│   │   │       └── game-board.component.ts
│   │   │
│   │   └── shared/                   ← Componentes compartidos
│   │       ├── components/
│   │       │   ├── card/
│   │       │   └── notification/
│   │       └── directives/
│   │           └── drag-drop.directive.ts
│   │
│   └── e2e/                          ← Tests End-to-End
│
├── docs/
│   ├── openapi.yaml                  ← Especificación OpenAPI (SDD — diseñar primero)
│   ├── architecture-diagram.png
│   └── design-decisions.md
│
├── sql/
│   ├── schema.sql                    ← DDL completo
│   └── seed-data.sql                 ← Mazo de ejemplo funcional (set xy1)
│
└── README.md                         ← Este archivo
```

---

## 3. Patrones de Diseño Aplicados

### State Pattern — Estados de la Partida

```java
// La partida delega el comportamiento según su estado actual
public interface GameState {
    GameState drawCard(Game game, String playerId);
    GameState playCard(Game game, String playerId, PlayCardCommand cmd);
    GameState attack(Game game, String playerId, AttackCommand cmd);
    GameState endTurn(Game game, String playerId);
}

// Cada estado implementa solo las transiciones válidas
public class ActiveState implements GameState {
    // Delega al TurnManager según la fase del turno actual
}

public class SetupState implements GameState {
    // Solo permite colocar Pokémon Activo y Banca
}
```

### Strategy Pattern — Efectos de Entrenadores

```java
public interface TrainerEffect {
    GameContext apply(GameContext context);
}

// Cada carta de Entrenador tiene su estrategia de efecto
public class ProfessorSycamoreEffect implements TrainerEffect {
    // Descartar mano, robar 7 cartas
}

public class PotionEffect implements TrainerEffect {
    // Curar 30 daño de un Pokémon propio
}
```

### Chain of Responsibility — Pipeline de Ataque

```java
public interface AttackStep {
    AttackContext process(AttackContext context, AttackStep next);
}

public class AttackPipeline {
    private final List<AttackStep> steps = List.of(
        new EnergyValidationStep(),
        new ConfusionCheckStep(),
        new SelectionStep(),
        new PreAttackStep(),
        new ModifierStep(),
        new DamageApplicationStep(),
        new PostDamageEffectStep()
    );
    
    public AttackContext execute(AttackContext context) { ... }
}
```

### Observer Pattern — Eventos via WebSocket

```java
// Puerto secundario: el Engine no sabe cómo se publican los eventos
public interface EventPublisher {
    void publish(String gameId, GameEvent event);
}

// Adaptador: la implementación usa WebSocket
public class WebSocketEventPublisher implements EventPublisher {
    public void publish(String gameId, GameEvent event) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, event);
    }
}
```

### Facade Pattern — Game Engine

```java
// API pública simple del Game Engine
// Los controllers REST y WebSocket solo usan esta interfaz
@Component
public class GameEngineFacade implements GameEnginePort {
    private final RuleValidator ruleValidator;
    private final DamageCalculator damageCalculator;
    private final StatusEffectManager statusEffectManager;
    private final VictoryConditionChecker victoryChecker;
    private final TurnManager turnManager;
    private final AttackPipeline attackPipeline;
    // ...
}
```

1. El jugador envía una acción (ej: AttackCommand)
2. GameEngine recibe la acción
3. RuleValidator valida la acción
4. TurnManager verifica si es válida en la fase actual
5. Se ejecuta la lógica correspondiente:
   - Attack → AttackPipeline
   - Draw → TurnManager
6. Se actualiza el estado del juego
7. VictoryConditionChecker evalúa condiciones
8. Se emiten eventos (Observer)
9. Se persiste el estado
---
