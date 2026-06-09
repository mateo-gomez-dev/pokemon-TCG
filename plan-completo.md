# PLAN COMPLETO — 7 PERSONAS, TRABAJO PARALELO DESDE DÍA 1

## CONTEXTO DEL PROYECTO

- **Estado actual:** Arquitectura lista, modelos de dominio creados, puertos definidos. **0% funcional**.
- **Meta:** Juego completamente funcional en 1 mes (14 días de trabajo intensivo + 14 buffer).
- **Metodología:** Cada persona usa un agente IA para implementar su parte siguiendo TDD.
- **Trabajo paralelo:** Todos empiezan Día 1 usando mocks e interfaces acordadas.

---

## DIVISIÓN DE RESPONSABILIDADES

| Rol | Persona | Responsabilidad | Semana Clave |
|-----|---------|-----------------|--------------|
| **P1 - Game Engine Core** | [Nombre] | GameEngineFacade + TurnManager + State Pattern | Semanas 1-2 |
| **P2 - Sistema de Combate** | [Nombre] | DamageCalculator + AttackPipeline (7 steps) | Semanas 1-2 |
| **P3 - Reglas y Estado** | [Nombre] | StatusEffectManager + RuleValidator + VictoryCondition | Semanas 1-2 |
| **P4 - Aplicación y Servicios** | [Nombre] | GameService + DeckService + MatchmakingService + DTOs | Semanas 1-3 |
| **P5 - Persistencia e Infra** | [Nombre] | Entidades JPA + Repositorios + PokemonTcgApi + FIX pom.xml | Semanas 1-3 |
| **P6 - API REST + WebSocket** | [Nombre] | Controllers + WebSocket + GlobalExceptionHandler | Semanas 1-3 |
| **P7 - Frontend Angular** | [Nombre] | Modelos TS + Servicios HTTP/WS + UI completa | Semanas 1-4 |

---

## PLAN DE INTEGRACIÓN PROGRESIVA

```
DÍA 1 (Todos empiezan):
┌─────────────────────────────────────────────────────────────┐
│ P1: Define interfaces TurnManager + State Pattern           │
│ P2: Define interfaces DamageCalculator + AttackStep         │
│ P3: Define interfaces StatusEffectManager + RuleValidator   │
│ P4: Crea TODOS los DTOs + interfaces de servicios + mocks   │
│ P5: FIX pom.xml + crea entidades JPA + repositorios Spring  │
│ P6: Crea esqueletos controllers (retornan 200 mock) + WS    │
│ P7: Crea modelos TS + servicios con MOCKS + Lobby UI        │
└─────────────────────────────────────────────────────────────┘

DÍA 2-5 (Implementación paralela con mocks):
┌─────────────────────────────────────────────────────────────┐
│ P1: GameEngineFacade + TurnManager + State Pattern          │
│ P2: DamageCalculator + AttackPipeline (7 steps)             │
│ P3: StatusEffectManager + RuleValidator + VictoryCondition  │
│ P4: GameService + DeckService + MatchmakingService (mocks)  │
│ P5: Adapters + ModelMapper + PokemonTcgApi + EventPublisher │
│ P6: Controllers completos con mocks + WebSocket config      │
│ P7: Lobby + Deck Builder + Game Board skeleton + Drag&Drop  │
└─────────────────────────────────────────────────────────────┘

DÍA 6-7 (Mini-integración BE):
┌─────────────────────────────────────────────────────────────┐
│ P1+P2+P3: Conectan engine components                        │
│ P4: Reemplaza mocks del engine con implementación real      │
│ P5: Conecta repositorios con servicios                      │
│ P6: Conecta controllers con servicios reales                │
└─────────────────────────────────────────────────────────────┘

DÍA 8-10 (Integración FE + BE):
┌─────────────────────────────────────────────────────────────┐
│ P7: Reemplaza mocks con API real                            │
│ P6: Verifica WebSocket + eventos                            │
│ TODOS: Tests E2E + bug fixes                                │
└─────────────────────────────────────────────────────────────┘

DÍA 11-14 (Polish + Demo):
┌─────────────────────────────────────────────────────────────┐
│ UI polish, animaciones, rendimiento                         │
│ Tests E2E completos                                         │
│ Preparar demo final                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## DEPENDENCIAS CRÍTICAS ENTRE PERSONAS

Para que **no haya conflictos al integrar**:

```
P4 define los DTOs → P6 los usa en controllers → P7 los mapea a TypeScript
P1 define GameEnginePort → P4 lo usa en GameService → P6 delega al servicio
P3 define RuleValidator → P4 lo usa en DeckService
P5 define repos adapters → P4 los usa en servicios
P5 define EventPublisher → P6 lo usa en WebSocket
P2 define DamageCalculator → P1 lo integra en GameEngineFacade
P3 define StatusEffectManager → P1 lo integra en GameEngineFacade
```

**Flujo de archivos entre personas:**

```
Día 1: P4 crea DTOs → comparte con P6 y P7
Día 1: P1,P2,P3 definen interfaces entre sí
Día 6: P1+P2+P3 se integran
Día 7: P4 conecta con P1(engine) + P5(repos)
Día 7: P6 conecta con P4(servicios) + P5(websocket)
Día 8: P7 conecta con P6(API real)
```

---

## GIT STRATEGY

Cada persona trabaja en su **propia rama**:
- `feat/p1-game-engine`
- `feat/p2-combat`
- `feat/p3-rules`
- `feat/p4-services`
- `feat/p5-persistence`
- `feat/p6-api`
- `feat/p7-frontend`

**Reglas:**
- Commits diarios con mensajes claros
- Merge a `dev` solo cuando pase `mvn test` (BE) o `ng test` (FE)
- Las carpetas de cada persona **NO se solapan** — no hay conflicto de merge si siguen la estructura definida
- Si alguien necesita cambiar un archivo de otra persona, **coordinar primero**

---

# PERSONA 1 — GAME ENGINE CORE

**Responsabilidad:** GameEngineFacade, TurnManager, State Pattern, Setup del juego

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | Leer contexto completo. Definir interfaz `TurnManager`. Definir interfaz `GameState` + estados. Escribir tests vacíos (RED). |
| Día 2-3 | Implementar `TurnManager`: manejar fases DRAW→MAIN→ATTACK→BETWEEN_TURNS. Tests: `TurnManagerTest`. Implementar State Pattern: `WaitingState`, `SetupState`, `ActiveState`, `FinishedState`. |
| Día 4-5 | Implementar `GameEngineFacade`: integrar `TurnManager`, `DamageCalculator`(P2), `StatusEffectManager`(P3). Implementar setup del juego: barajar, robar 7, mulligan, colocar Pokémon, cartas de premio, moneda. |
| Día 6-7 | Conectar con P2 y P3. Resolver integración del engine completo. Tests de integración. |
| Día 8-10 | Fix de bugs de integración. Verificar que `GameEnginePort` se implemente correctamente. |
| Día 11-14 | Tests E2E del engine. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/DOMAIN.md (completo)
📄 docs/TESTING.md
📄 BE/AGENTS.md

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/in/GameEnginePort.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Game.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Player.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Turn.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TurnPhase.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TurnState.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Board.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Hand.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Deck.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Card.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PokemonCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/EnergyCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TrainerCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/HP.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/DamageCounter.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/SpecialCondition.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PrizeCards.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/ActivePokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/BenchPokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/EnergyAttachment.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/GameRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/RandomProvider.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/EventPublisher.java
```

## CONTRATO QUE DEBE RESPETAR (para que P4 no tenga conflictos)

```java
// GameEnginePort YA EXISTE — NO CAMBIAR la interfaz
// Si necesita agregar métodos, coordinar con P4 primero

public interface GameEnginePort {
    Game setupGame(String gameId, String player1Id, String player2Id, Deck deck1, Deck deck2);
    Game drawCard(String gameId, String playerId);
    Game playCard(String gameId, String playerId, PlayCardCommand command);
    Game attachEnergy(String gameId, String playerId, AttachEnergyCommand command);
    Game declareAttack(String gameId, String playerId, AttackCommand command);
    Game retreat(String gameId, String playerId, RetreatCommand command);
    Game endTurn(String gameId, String playerId);
    Game processBetweenTurns(String gameId);
    Game reconnect(String gameId, String playerId);
}

// Los command records YA ESTÁN definidos en GameEnginePort — USARLOS
// PlayCardCommand, AttachEnergyCommand, AttackCommand, RetreatCommand
```

## ARCHIVOS QUE CREARÁ

```
BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/engine/
├── GameEngineFacade.java          ← implementa GameEnginePort
├── TurnManager.java
├── GameState.java                 ← interfaz
├── states/
│   ├── WaitingState.java
│   ├── SetupState.java
│   ├── ActiveState.java
│   └── FinishedState.java
└── exceptions/
    └── GameActionException.java

BE/src/test/java/ar/edu/utn/frc/tup/piii/domain/engine/
├── GameEngineFacadeTest.java
├── TurnManagerTest.java
└── GameStateTest.java
```

---

# PERSONA 2 — SISTEMA DE COMBATE

**Responsabilidad:** DamageCalculator, AttackPipeline (Chain of Responsibility), 7 Steps

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | Leer contexto completo. Definir interfaz `DamageCalculator`. Definir interfaz `AttackStep` y `AttackContext`. Escribir tests vacíos (RED). |
| Día 2-3 | Implementar `DamageCalculator`: dañoBase × debilidad(×2) - resistencia(-20) + modificadores. Mínimo 0. Contadores de daño (1 cada 10pts). Tests: `DamageCalculatorTest`. |
| Día 4-5 | Implementar `AttackPipeline` con Chain of Responsibility. Implementar los 7 steps uno por uno con TDD. Tests individuales para cada step. |
| Día 6-7 | Integrar con P1. Conectar pipeline al `GameEngineFacade`. Tests de integración del pipeline completo. |
| Día 8-10 | Fix de bugs de integración. Verificar casos edge (daño 0, debilidad + resistencia, etc). |
| Día 11-14 | Tests E2E de combate. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/DOMAIN.md (secciones 1.1, 1.2, 1.4 — TIPOS DE CARTAS, CONDICIONES, PIPELINE)
📄 docs/TESTING.md

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PokemonCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Card.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/HP.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/DamageCounter.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/SpecialCondition.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/ActivePokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/BenchPokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/EnergyCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/EnergyAttachment.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/in/GameEnginePort.java  ← para ver AttackCommand
```

## CONTRATO QUE DEBE RESPETAR

```java
// DamageCalculator debe ser una clase pura (sin Spring)
// Método público:
public class DamageCalculator {
    public int calculate(int baseDamage, PokemonCard attacker, PokemonCard defender, AttackContext context);
}

// AttackPipeline usa Chain of Responsibility
public interface AttackStep {
    void process(AttackContext context);
}

// AttackContext lleva toda la info del ataque
public class AttackContext {
    // attacker, defender, attack, currentGame, damageDealt, etc.
}
```

## ARCHIVOS QUE CREARÁ

```
BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/engine/
├── DamageCalculator.java
├── attack/
│   ├── AttackPipeline.java
│   ├── AttackContext.java
│   ├── AttackStep.java               ← interfaz
│   └── steps/
│       ├── EnergyValidationStep.java
│       ├── ConfusionCheckStep.java
│       ├── SelectionStep.java
│       ├── PreAttackStep.java
│       ├── ModifierStep.java
│       ├── DamageApplicationStep.java
│       └── PostDamageEffectStep.java
└── exceptions/
    └── AttackException.java

BE/src/test/java/ar/edu/utn/frc/tup/piii/domain/engine/
├── DamageCalculatorTest.java
└── attack/
    ├── AttackPipelineTest.java
    └── steps/
        └── (un test por cada step)
```

---

# PERSONA 3 — REGLAS Y ESTADO

**Responsabilidad:** StatusEffectManager, RuleValidator, VictoryConditionChecker, TrainerEffects

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | Leer contexto completo. Definir interfaz `StatusEffectManager`, `RuleValidator`, `VictoryConditionChecker`. Escribir tests vacíos (RED). |
| Día 2-3 | Implementar `StatusEffectManager`: procesar condiciones en orden (Envenenado→Quemado→Dormido→Paralizado), exclusividad, eliminar al retirarse/evolucionar. Tests: `StatusEffectManagerTest`. |
| Día 4-5 | Implementar `RuleValidator`: validaciones de mazo (60 cartas, 4 copias, 1 Ace Spec, 1 Básico, set xy1), validaciones de acciones (1 energía/turno, 1 partidario/turno, 1 retiro/turno, máx 5 banca), restricciones de evolución. Tests: `RuleValidatorTest` con cobertura ≥90%. Implementar `VictoryConditionChecker`: 3 condiciones de victoria. |
| Día 6-7 | Integrar con P1. Conectar al `GameEngineFacade`. Implementar `TrainerEffect` (Strategy Pattern). Tests de integración. |
| Día 8-10 | Fix de bugs de integración. Verificar todas las reglas del juego. |
| Día 11-14 | Tests E2E de reglas. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/DOMAIN.md (secciones 1.2, 1.3, 1.5, 1.6 — CONDICIONES, TURNO, SETUP, MAZO)
📄 docs/TESTING.md

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/SpecialCondition.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Deck.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Hand.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Board.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TurnState.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TurnPhase.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Turn.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Game.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Player.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PrizeCards.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/ActivePokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/BenchPokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PokemonCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TrainerCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Card.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/DamageCounter.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/in/GameEnginePort.java
```

## CONTRATO QUE DEBE RESPETAR

```java
// StatusEffectManager — clase pura (sin Spring)
public class StatusEffectManager {
    public void processBetweenTurns(ActivePokemon pokemon);
    public void applyCondition(ActivePokemon pokemon, SpecialCondition condition);
    public void removeConditionsOnRetreat(ActivePokemon pokemon);
    public void removeConditionsOnEvolution(ActivePokemon pokemon);
}

// RuleValidator — clase pura (sin Spring)
public class RuleValidator {
    public void validateDeck(Deck deck);
    public void validateEvolution(PokemonCard base, PokemonCard evolution, Turn turn);
    public void validateEnergyAttachment(TurnState state);
    public void validateRetreat(TurnState state);
    public void validateSupporter(TurnState state);
    public void validateAttackAllowed(Turn turn, boolean isFirstTurnOfPlayer);
}

// VictoryConditionChecker — clase pura (sin Spring)
public class VictoryConditionChecker {
    public Optional<String> checkVictory(Game game); // retorna playerId o empty
}
```

## ARCHIVOS QUE CREARÁ

```
BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/engine/
├── StatusEffectManager.java
├── RuleValidator.java
├── VictoryConditionChecker.java
├── effects/
│   ├── TrainerEffect.java            ← interfaz
│   ├── ItemEffect.java
│   ├── SupporterEffect.java
│   ├── StadiumEffect.java
│   └── ToolEffect.java
└── exceptions/
    ├── DeckValidationException.java
    └── RuleViolationException.java

BE/src/test/java/ar/edu/utn/frc/tup/piii/domain/engine/
├── StatusEffectManagerTest.java
├── RuleValidatorTest.java
├── VictoryConditionCheckerTest.java
└── effects/
    └── (tests de efectos)
```

---

# PERSONA 4 — APLICACIÓN Y SERVICIOS

**Responsabilidad:** GameService, DeckService, MatchmakingService, ReconnectionService, CardCacheService, TODOS los DTOs

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | Leer contexto completo. **CREAR TODOS LOS DTOs** — esto es CRÍTICO porque P6 y P7 los necesitan. Definir interfaces de servicios. Implementar servicios con MOCKS (datos hardcodeados). |
| Día 2-3 | Implementar `DeckService` con validación: CRUD de mazos, agregar/quitar cartas, validar mazo usando `RuleValidator` (de P3, usar interfaz). Implementar `MatchmakingService`: lobby, crear partida, unirse. |
| Día 4-5 | Implementar `GameService`: orquestar `GameEnginePort` con persistencia. Delegar acciones al engine. Implementar `ReconnectionService` y `CardCacheService`. Tests con Mockito. |
| Día 6-7 | Reemplazar mocks con implementación real de P1 (engine), P3 (RuleValidator), P5 (repositorios). Integration tests. |
| Día 8-10 | Fix de bugs de integración. Verificar que DTOs coincidan con lo que espera P6 y P7. |
| Día 11-14 | Tests E2E de servicios. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/DOMAIN.md (completo)
📄 docs/API.md (completo — define endpoints que los servicios deben exponer)
📄 docs/TESTING.md

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/in/GameEnginePort.java  ← LA INTERFAZ CLAVE
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/GameRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/DeckRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/CardCacheRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Game.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Player.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Deck.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Card.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PokemonCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/EnergyCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TrainerCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Hand.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Board.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PrizeCards.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Turn.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TurnPhase.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TurnState.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/HP.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/DamageCounter.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/SpecialCondition.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/ActivePokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/BenchPokemon.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/common/ErrorApi.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/configs/MappersConfig.java
```

## CONTRATO QUE DEBE RESPETAR (CRÍTICO — P6 y P7 dependen de esto)

```java
// DTOs — ESTOS SON LOS QUE P6 expone en la API y P7 consume
// NUNCA exponer modelos de dominio directamente

// GameStateResponse — lo que el frontend ve del juego
public class GameStateResponse {
    private String gameId;
    private String currentPlayerId;
    private PlayerViewResponse myView;       // MI info completa
    private OpponentViewResponse opponentView; // info del rival (SIN ver su mano)
    private String phase;                     // DRAW, MAIN, ATTACK, BETWEEN_TURNS
    private String gameState;                 // WAITING, SETUP, ACTIVE, FINISHED
    private List<GameLogEntryResponse> log;
}

// PlayerViewResponse — info de UN jugador
public class PlayerViewResponse {
    private String playerId;
    private String username;
    private List<CardResponse> hand;          // SOLO la mano del jugador actual
    private ActivePokemonResponse activePokemon;
    private List<BenchPokemonResponse> bench;
    private int prizesRemaining;
    private DeckInfoResponse deckInfo;        // solo cantidad de cartas restantes
}

// CardResponse — carta simplificada para la UI
public class CardResponse {
    private String id;
    private String name;
    private String type;                      // Pokémon, Energy, Trainer
    private List<String> subtypes;
    private String imageUrl;
    // campos específicos según tipo
    private Integer hp;                       // solo si es Pokémon
    private List<AttackResponse> attacks;     // solo si es Pokémon
    private String weaknessType;
    private String resistanceType;
    private Integer retreatCost;
    private Integer damageCounters;           // si está en juego
    private List<String> conditions;          // condiciones especiales
    private List<String> attachedEnergies;
}

// GameActionResponse — respuesta genérica de una acción
public class GameActionResponse {
    private boolean success;
    private String message;
    private GameStateResponse gameState;      // estado actualizado
}

// ErrorResponse — manejo de errores
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}

// LobbyResponse
public class LobbyResponse {
    private List<GameSummaryResponse> availableGames;
}

public class GameSummaryResponse {
    private String gameId;
    private String player1Username;
    private String player2Username;           // null si solo hay 1
    private LocalDateTime createdAt;
}
```

## ARCHIVOS QUE CREARÁ

```
BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/
├── request/
│   ├── CreateGameRequest.java
│   ├── JoinGameRequest.java
│   ├── CreateDeckRequest.java
│   ├── UpdateDeckRequest.java
│   ├── AddCardToDeckRequest.java
│   └── PlayCardRequest.java
├── response/
│   ├── GameStateResponse.java
│   ├── PlayerViewResponse.java
│   ├── OpponentViewResponse.java
│   ├── CardResponse.java
│   ├── AttackResponse.java
│   ├── ActivePokemonResponse.java
│   ├── BenchPokemonResponse.java
│   ├── DeckInfoResponse.java
│   ├── GameActionResponse.java
│   ├── GameSummaryResponse.java
│   ├── LobbyResponse.java
│   ├── DeckResponse.java
│   ├── GameLogEntryResponse.java
│   └── ErrorResponse.java
└── common/
    └── ErrorApi.java                        ← YA EXISTE

BE/src/main/java/ar/edu/utn/frc/tup/piii/application/
├── GameService.java
├── DeckService.java
├── MatchmakingService.java
├── ReconnectionService.java
├── CardCacheService.java
└── exceptions/
    ├── GameNotFoundException.java
    ├── DeckNotFoundException.java
    ├── PlayerNotInGameException.java
    └── ActionNotAllowedException.java

BE/src/test/java/ar/edu/utn/frc/tup/piii/application/
├── GameServiceTest.java
├── DeckServiceTest.java
├── MatchmakingServiceTest.java
└── (tests de cada servicio)
```

---

# PERSONA 5 — PERSISTENCIA E INFRAESTRUCTURA

**Responsabilidad:** Entidades JPA, Repositorios, Adapters, PokemonTcgApi, WebSocketEventPublisher, RandomProvider, FIX pom.xml

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | **FIX pom.xml** (5 cambios rápidos). Leer contexto completo. Crear entidades JPA mapeando las tablas del `sql/schema.sql`. Configurar Spring Data repos. |
| Día 2-3 | Implementar adapters: `GameRepositoryAdapter`, `DeckRepositoryAdapter`, `CardCacheRepositoryAdapter`. Mapear Entidades↔Dominio con ModelMapper. |
| Día 4-5 | Implementar `PokemonTcgApiAdapter` (cliente HTTP a pokemontcg.io). Implementar `WebSocketEventPublisher` (implementa `EventPublisher`). Implementar `SpringRandomProvider`. |
| Día 6-7 | Conectar adapters con servicios de P4. Tests de integración con H2. Verificar que `GameRepository` (puerto out) se implemente correctamente. |
| Día 8-10 | Fix de bugs. Verificar JSONB para game_state_json. Verificar game_logs inmutable. |
| Día 11-14 | Tests de persistencia. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/DATABASE.md
📄 sql/schema.sql
📄 sql/seed-data.sql
📄 BE/AGENTS.md

📄 BE/pom.xml                                  ← para hacer los FIXES
📄 BE/src/main/resources/application.properties

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/GameRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/DeckRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/CardCacheRepository.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/EventPublisher.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/port/out/RandomProvider.java

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Game.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Player.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Deck.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/Card.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/PokemonCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/EnergyCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/domain/model/TrainerCard.java
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/configs/MappersConfig.java
```

## CONTRATO QUE DEBE RESPETAR

```java
// Los puertos out YA EXISTEN — IMPLEMENTARLOS, no cambiarlos

public interface GameRepository {
    Optional<Game> findById(String gameId);
    Game save(Game game);
    List<Game> findByState(Game.GameState state);
    void delete(String gameId);
}

public interface DeckRepository {
    Optional<Deck> findById(String deckId);
    List<Deck> findByPlayerId(String playerId);
    Deck save(Deck deck);
    void delete(String deckId);
}

public interface CardCacheRepository {
    Optional<Card> findById(String cardId);
    List<Card> findBySetId(String setId);
    void saveAll(List<Card> cards);
}

public interface EventPublisher {
    void publish(String topic, Object event);
    void publishToUser(String userId, String topic, Object event);
}

public interface RandomProvider {
    boolean flipCoin();  // true=cara, false=cruz
}
```

## ARCHIVOS QUE CREARÁ

```
BE/src/main/java/ar/edu/utn/frc/tup/piii/infrastructure/
├── persistence/
│   ├── entity/
│   │   ├── GameEntity.java
│   │   ├── PlayerEntity.java
│   │   ├── DeckEntity.java
│   │   ├── DeckCardEntity.java
│   │   ├── CardEntity.java
│   │   ├── GameLogEntity.java
│   │   └── PrizeCardEntity.java
│   ├── springdata/
│   │   ├── GameJpaRepository.java        ← extends JpaRepository
│   │   ├── DeckJpaRepository.java
│   │   └── CardJpaRepository.java
│   └── adapter/
│       ├── GameRepositoryAdapter.java    ← implementa GameRepository
│       ├── DeckRepositoryAdapter.java    ← implementa DeckRepository
│       └── CardCacheRepositoryAdapter.java ← implementa CardCacheRepository
├── external/
│   ├── PokemonTcgApiAdapter.java         ← cliente HTTP pokemontcg.io
│   └── PokemonTcgApiClient.java
├── websocket/
│   └── WebSocketEventPublisher.java      ← implementa EventPublisher
└── random/
    └── SpringRandomProvider.java         ← implementa RandomProvider

BE/src/test/java/ar/edu/utn/frc/tup/piii/infrastructure/
└── (tests de adapters)

FIXES EN BE/pom.xml:
1. springdoc-openapi: cambiar <version>2.8.0</version> → <version>${springdoc-openapi.version}</version>
2. Lombok: cambiar 1.18.30 → 1.18.34
3. PMD: cambiar 7.0.0-rc3 → 7.0.0
4. Spring Boot: cambiar 4.0.0 → 4.0.6
5. Eliminar Application.java duplicado (dejar PokemonApplication.java)
```

---

# PERSONA 6 — API REST + WEBSOCKET

**Responsabilidad:** TODOS los Controllers, WebSocket Config, GlobalExceptionHandler

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | Leer contexto completo. Crear esqueletos de TODOS los controllers con endpoints que retornan 200 OK + datos mock. Configurar WebSocket (STOMP/SockJS). Crear `GlobalExceptionHandler`. |
| Día 2-3 | Implementar `CardController`: GET /api/cards, POST /api/cards/sync. Implementar `DeckController`: CRUD completo + validate. Implementar `LobbyController`: GET /api/lobby, POST /api/games, POST /api/games/{id}/join. |
| Día 4-5 | Implementar `GameController`: GET estado, GET log, POST todas las acciones (draw, play-card, attach-energy, evolve, use-ability, retreat, attack, end-turn). |
| Día 6-7 | Conectar controllers con servicios reales de P4. Conectar WebSocket con `EventPublisher` de P5. |
| Día 8-10 | Fix de bugs de integración. Verificar manejo de errores HTTP (400, 403, 404, 409, 500). Probar con Postman/Swagger. |
| Día 11-14 | Tests de integración API. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/API.md (completo — define TODOS los endpoints)
📄 docs/TESTING.md

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/GameStateResponse.java       ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/PlayerViewResponse.java      ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/CardResponse.java            ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/GameActionResponse.java      ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/ErrorResponse.java           ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/LobbyResponse.java           ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/GameSummaryResponse.java     ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/response/DeckResponse.java            ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/CreateGameRequest.java        ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/JoinGameRequest.java          ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/CreateDeckRequest.java        ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/UpdateDeckRequest.java        ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/AddCardToDeckRequest.java     ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/request/PlayCardRequest.java          ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/dtos/common/ErrorApi.java

📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/application/GameService.java               ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/application/DeckService.java               ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/application/MatchmakingService.java        ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/application/ReconnectionService.java       ← de P4
📄 BE/src/main/java/ar/edu/utn/frc/tup/piii/application/CardCacheService.java          ← de P4
```

## CONTRATO QUE DEBE RESPETAR

```
ENDPOINTS EXACTOS (NO CAMBIAR rutas):

CardController:
  GET    /api/cards?setId=xy1&name=...
  POST   /api/cards/sync

DeckController:
  GET    /api/decks
  POST   /api/decks
  GET    /api/decks/{deckId}
  PUT    /api/decks/{deckId}
  DELETE /api/decks/{deckId}
  POST   /api/decks/{deckId}/validate
  POST   /api/decks/{deckId}/cards
  DELETE /api/decks/{deckId}/cards/{cardId}

LobbyController:
  GET    /api/lobby
  POST   /api/games
  POST   /api/games/{gameId}/join
  GET    /api/games/{gameId}

GameController:
  GET    /api/games/{gameId}/log
  POST   /api/games/{gameId}/actions/draw
  POST   /api/games/{gameId}/actions/play-card
  POST   /api/games/{gameId}/actions/attach-energy
  POST   /api/games/{gameId}/actions/evolve
  POST   /api/games/{gameId}/actions/use-ability
  POST   /api/games/{gameId}/actions/retreat
  POST   /api/games/{gameId}/actions/attack
  POST   /api/games/{gameId}/actions/end-turn

WebSocket:
  URL: ws://localhost:8080/ws
  Subscribe: /topic/game/{gameId}, /user/queue/game/{gameId}, /topic/lobby
  Send: /app/game/{gameId}/action, /app/lobby/join
```

## ARCHIVOS QUE CREARÁ

```
BE/src/main/java/ar/edu/utn/frc/tup/piii/controllers/
├── PingController.java                        ← YA EXISTE
├── CardController.java
├── DeckController.java
├── LobbyController.java
├── GameController.java
└── exception/
    └── GlobalExceptionHandler.java

BE/src/main/java/ar/edu/utn/frc/tup/piii/configs/
├── WebSocketConfig.java
├── MappersConfig.java                         ← YA EXISTE
└── SpringDocConfig.java                       ← YA EXISTE

BE/src/test/java/ar/edu/utn/frc/tup/piii/controllers/
├── PingControllerTest.java                    ← YA EXISTE
├── CardControllerTest.java
├── DeckControllerTest.java
├── LobbyControllerTest.java
└── GameControllerTest.java
```

---

# PERSONA 7 — FRONTEND ANGULAR

**Responsabilidad:** Modelos TypeScript, Servicios HTTP/WS, Lobby, Deck Builder, Game Board UI completa

## TAREAS

| Día | Tarea |
|-----|-------|
| Día 1 | Leer contexto completo. **CREAR TODOS LOS MODELOS TS** mapeando los DTOs de P4. **CREAR SERVICIOS HTTP con MOCKS** (datos hardcodeados). Configurar WebSocket service. |
| Día 2-3 | Implementar **Lobby**: crear partida, unirse, ver disponibles. Implementar **Deck Builder**: buscar cartas, armar mazo, validar en tiempo real. |
| Día 4-5 | Implementar **Game Board**: componentes `active-pokemon`, `bench`, `hand`, `prize-cards`, `opponent-view`, `action-panel`, `game-log`. Usar Angular Signals. |
| Día 6-7 | Implementar **Drag & Drop**: arrastrar Pokémon a banca, energía a Pokémon, herramienta, entrenador. Highlight de targets válidos. |
| Día 8-10 | **Conectar con API real**: reemplazar mocks con endpoints de P6. Conectar WebSocket para tiempo real. Reconexión automática. |
| Día 11-14 | UI polish, animaciones básicas, notificaciones visuales. Tests E2E. Fix finales. |

## ARCHIVOS QUE DEBE PASARLE A SU IA

```
📄 docs/ARCHITECTURE.md
📄 docs/DOMAIN.md (completo — para entender las reglas del juego)
📄 docs/API.md (completo — define endpoints y eventos WebSocket)
📄 FE/AGENTS.md

📄 FE/src/app/app.config.ts
📄 FE/src/app/app.routes.ts
📄 FE/src/app/app.ts
📄 FE/angular.json
📄 FE/package.json

# Los DTOs del backend que debe mapear a TypeScript:
# (estos los define P4, pero pueden acordarse el Día 1)
# GameStateResponse, PlayerViewResponse, CardResponse, etc.
```

## CONTRATO QUE DEBE RESPETAR

```typescript
// Modelos TS — deben mapear los DTOs de P4 exactamente

export interface GameStateResponse {
  gameId: string;
  currentPlayerId: string;
  myView: PlayerViewResponse;
  opponentView: OpponentViewResponse;
  phase: 'DRAW' | 'MAIN' | 'ATTACK' | 'BETWEEN_TURNS';
  gameState: 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED';
  log: GameLogEntryResponse[];
}

export interface PlayerViewResponse {
  playerId: string;
  username: string;
  hand: CardResponse[];
  activePokemon: ActivePokemonResponse | null;
  bench: BenchPokemonResponse[];
  prizesRemaining: number;
  deckInfo: DeckInfoResponse;
}

export interface CardResponse {
  id: string;
  name: string;
  type: 'Pokémon' | 'Energy' | 'Trainer';
  subtypes: string[];
  imageUrl: string;
  hp?: number;
  attacks?: AttackResponse[];
  weaknessType?: string;
  resistanceType?: string;
  retreatCost?: number;
  damageCounters?: number;
  conditions?: string[];
  attachedEnergies?: string[];
}

// WebSocket events
export interface WebSocketEvent {
  type: 'GAME_STATE_UPDATE' | 'TURN_START' | 'POKEMON_KO' | 'PRIZE_TAKEN' |
        'SPECIAL_CONDITION' | 'GAME_OVER' | 'SUDDEN_DEATH' | 'RECONNECT_STATE';
  payload: any;
}
```

## ARCHIVOS QUE CREARÁ

```
FE/src/app/core/
├── models/
│   ├── game-state.model.ts
│   ├── card.model.ts
│   ├── player-view.model.ts
│   ├── websocket-event.model.ts
│   └── game-action.model.ts
├── services/
│   ├── game.service.ts
│   ├── deck.service.ts
│   ├── websocket.service.ts
│   └── auth.service.ts
│   └── mock-data.ts                    ← datos mock para Día 1-7
├── guards/
│   └── game.guard.ts
└── interceptors/
    └── error.interceptor.ts

FE/src/app/features/
├── lobby/
│   ├── lobby-page/
│   │   ├── lobby-page.ts
│   │   ├── lobby-page.html
│   │   └── lobby-page.css
│   └── game-list/
│       ├── game-list.ts
│       ├── game-list.html
│       └── game-list.css
├── deck-builder/
│   ├── deck-builder-page/
│   │   ├── deck-builder-page.ts
│   │   ├── deck-builder-page.html
│   │   └── deck-builder-page.css
│   ├── card-search/
│   └── deck-editor/
└── game-board/
    ├── game-board-page/
    │   ├── game-board-page.ts
    │   ├── game-board-page.html
    │   └── game-board-page.css
    ├── active-pokemon/
    ├── bench/
    ├── hand/
    ├── prize-cards/
    ├── opponent-view/
    ├── action-panel/
    └── game-log/

FE/src/app/shared/
├── components/
│   └── card-thumbnail/
└── ui/
    └── notification/
```

---

# CHECKLIST SEMANAL (revisar cada viernes)

## Semana 1 — Fin

- [ ] P1: GameEngineFacade con tests
- [ ] P2: DamageCalculator con tests
- [ ] P3: StatusEffectManager + RuleValidator con tests
- [ ] P4: TODOS los DTOs creados + servicios con mocks
- [ ] P5: FIX pom.xml + Entidades JPA + Repositorios
- [ ] P6: Esqueletos controllers + WebSocket config
- [ ] P7: Modelos TS + servicios mock + Lobby UI

## Semana 2 — Fin

- [ ] P1: State Pattern + Setup completo + conectado con P2+P3
- [ ] P2: AttackPipeline (7 steps) con tests
- [ ] P3: VictoryCondition + TrainerEffects con tests
- [ ] P4: GameService + DeckService con tests + conectado con engine
- [ ] P5: Adapters + PokemonTcgApi + EventPublisher
- [ ] P6: Todos los controllers REST + conectado con servicios
- [ ] P7: Deck Builder + Game Board skeleton + Drag & Drop

## Semana 3 — Fin

- [ ] P1-P3: Engine completo integrado y testeado
- [ ] P4: Todos los servicios integrados
- [ ] P5: Persistencia completa testeada
- [ ] P6: WebSocket funcional + GlobalExceptionHandler
- [ ] P7: Game Board completo + WebSocket conectado

## Semana 4 — Fin

- [ ] Integración total funcional
- [ ] Tests E2E pasando
- [ ] Demo lista
- [ ] Bug fixes + polish final

---

# RECOMENDACIONES FINALES

1. **Todos deben leer** `ARCHITECTURE.md`, `DOMAIN.md` y `TESTING.md` ANTES de empezar. Obligatorio.

2. **Cada persona usa su propio agente IA** en sesiones separadas. No mezclar tareas en la misma sesión.

3. **Commit frecuente**: Después de cada tarea completada, hacer commit con mensaje descriptivo. Ejemplo: `feat: implement DamageCalculator with tests`

4. **No adelantar**: Si P4 intenta hacer GameService antes de que P1 tenga el engine, va a romper cosas. Respetar las dependencias.

5. **P7 puede usar mocks**: Mientras P6 no termine los controllers, P7 puede crear servicios mock para avanzar con la UI.

6. **Reunión diaria de 15 min**: Coordinar avances y bloqueos. Usar `PROGRESS.md` como referencia.

7. **El agente IA es tu herramienta, no tu reemplazo**: El agente genera código, pero la persona debe entender qué hace y verificarlo.

8. **Día 1: Reunión de 1 hora entre TODOS** — P4 presenta los DTOs a P6 y P7. P1 presenta interfaces de engine a P2 y P3. Todos acuerdan contratos antes de empezar.
