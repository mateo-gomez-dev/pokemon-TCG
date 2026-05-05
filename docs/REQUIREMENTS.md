## Índice

1. [Requerimientos Funcionales](#1-requerimientos-funcionales)
2. [Requerimientos No Funcionales](#2-requerimientos-no-funcionales)

## 1. Requerimientos Funcionales

### RF-01 — Reglas del Juego *(Prioridad: Crítica)*

El motor de juego completo debe implementarse **exclusivamente en el backend**. El frontend solo presenta estado recibido del servidor, nunca valida ni decide.

- [x] Preparación de la partida con mulligan
- [x] Ciclo completo de turno (DRAW → MAIN → ATTACK → BETWEEN_TURNS)
- [x] Pipeline de resolución de ataque (7 pasos)
- [x] Proceso de Knockout con reemplazo obligatorio
- [x] Condiciones especiales (Dormido, Confundido, Paralizado, Envenenado, Quemado)
- [x] Gestión de incompatibilidades entre condiciones especiales
- [x] Orden fijo de procesamiento entre turnos
- [x] Condiciones de victoria y derrota (incluyendo Muerte Súbita)
- [x] Validación estricta de TODAS las acciones en backend

### RF-02 — Tipos de Cartas *(Prioridad: Alta)*

- [x] Modelar y aplicar reglas de cada tipo: Pokémon (Básico/F1/F2), Pokémon-EX, Energía Básica, Energía Especial, Objeto, AS TÁCTICO, Partidario, Estadio, Herramienta
- [x] Validar restricciones de evolución (turno de entrada, primer turno del jugador)
- [x] Regla especial de Pokémon-EX (2 cartas de Premio al KO)
- [ ] *(Opcional)* Pokémon Megaevolución

### RF-03 — Gestión del Juego *(Prioridad: Alta)*

- [x] Ciclo de vida completo: creación → matchmaking → turnos → fin
- [x] Estados del juego: WAITING → SETUP → ACTIVE → FINISHED
- [x] El backend es la única fuente de verdad
- [x] Log completo e inmutable de eventos (turno, jugador, acción, resultado)
- [x] Persistencia automática del estado tras cada acción relevante
- [x] Los datos de cartas provienen del caché local (NO llamadas a API durante partida)
- [ ] *(Opcional)* Chat entre jugadores
- [ ] *(Opcional)* Sistema de ranking / historial de partidas

### RF-04 — Construcción de Mazos (Deck Builder) *(Prioridad: Alta)*

- [x] Interfaz para buscar cartas filtrando por set `xy1` (pokemontcg.io v2)
- [x] Agregar/quitar cartas al mazo con validaciones en tiempo real
- [x] Validación completa del mazo (60 cartas, máx. 4 copias, 1 AS TÁCTICO, 1 Básico mínimo)
- [x] Persistencia de mazos por jugador (CRUD: crear, editar, eliminar, listar)
- [x] Caché local obligatorio para no llamar a la API durante la partida
- [x] Interfaz visual: contador (0/60), límites por carta, errores descriptivos y accionables

### RF-05 — Guardado y Persistencia del Estado *(Prioridad: Alta)*

El estado persistido tras cada acción debe incluir **como mínimo**:

- Tablero completo: Pokémon Activo y Banca de cada jugador con todas sus cartas unidas
- Manos de ambos jugadores
- Mazos (incluyendo el orden)
- Pilas de descarte de ambos jugadores
- Cartas de Premio de cada jugador
- Contadores de daño sobre cada Pokémon en juego
- Condiciones especiales activas sobre cada Pokémon
- Flags del turno: fase actual, si ya unió Energía, si ya retiró, si ya jugó Partidario, etc.

### RF-06 — Comunicación en Tiempo Real *(Prioridad: Alta)*

- [x] WebSockets bidireccionales (tecnología recomendada: STOMP sobre SockJS con Spring)
- [x] Sincronizar estado tras cada acción válida (enviar a ambos clientes)
- [x] Notificaciones de eventos: inicio de turno, KO, toma de Premio, condiciones especiales, fin de partida
- [x] Reconexión robusta: el cliente desconectado recibe el estado actual al reconectarse
- [x] El frontend **nunca** modifica el estado localmente; solo presenta lo que indica el servidor

### RF-07 — Interfaz de Usuario *(Prioridad: Alta)*

- [x] Lobby: crear partida nueva / unirse a partida disponible
- [x] Tablero interactivo con zonas claramente diferenciadas:
  - Zona del oponente: Pokémon Activo, Banca, mazo, Premio, descarte
  - Zona del jugador: ídem
  - Zona compartida: Estadio activo
  - Mano del jugador
- [x] Drag & Drop para: colocar Básico en Banca, unir Energía, equipar Herramienta, jugar Entrenador (con selección de target cuando corresponda)
- [x] Visualización en tiempo real: HP actual/máximo, contadores de daño, Energías unidas, Herramienta equipada, condición especial activa (rotación de carta para Dormido/Confundido/Paralizado; marcador para Quemado/Envenenado), premios restantes, cantidad de cartas en mano del oponente (sin revelar contenido)
- [x] Panel de acciones con botones habilitados/deshabilitados según la fase y las acciones ya realizadas
- [x] Log de acciones visible con historial cronológico
- [x] Notificaciones visuales para eventos relevantes
- [x] Visualización y uso de Habilidades (Abilities) de Pokémon desde el tablero
- [ ] *(Opcional)* Animaciones para ataques, evoluciones y knockouts

---

## 2. Requerimientos No Funcionales

### RNF-01 — Rendimiento y Optimización

| Métrica | Umbral |
|---|---|
| Tiempo de respuesta de acciones de juego | < **200ms** promedio |
| Búsqueda de cartas (caché local) | < **500ms** |
| Búsqueda de cartas (API externa) | < **500ms** |
| Renderizado del tablero | Fluido, sin bloqueos perceptibles |

- Imágenes de cartas servidas en formatos eficientes (WebP preferido)
- Caché HTTP para recursos estáticos
- Lazy loading de módulos Angular

### RNF-02 — Calidad del Código y Buenas Prácticas

**Backend (Java 21):**
- Convenciones de nomenclatura Java estándar
- Principios SOLID aplicados, especialmente **Single Responsibility**
- El Game Engine completamente aislado: sin imports de Spring, JPA ni ningún framework
- Maven como gestor de dependencias y construcción

**Frontend (Angular 21+):**
- TypeScript con tipado estricto (`strict: true` en `tsconfig.json`)
- Arquitectura por componentes y servicios
- Guía de estilo oficial de Angular
- Reactive programming con RxJS

**Compatibilidad:** Chrome, Firefox, Safari y Edge. Dispositivos de escritorio y tablet.

### RNF-03 — Pruebas Unitarias y Cobertura

```
Backend:
├── JUnit 5 + Mockito para tests unitarios
├── JaCoCo para medición de cobertura
├── Cobertura mínima global: 80%
├── RuleValidator: ≥ 90%
├── DamageCalculator: ≥ 90%
├── StatusEffectManager: ≥ 90%
├── Tests de integración: partida completa, mulligan múltiple, evolución, KO, victoria
└── Reporte JaCoCo generado en: backend/target/site/jacoco/index.html

Frontend:
└── Tests E2E cubriendo: crear mazo → unirse a partida → ejecutar un turno
```

### RNF-04 — Patrones de Diseño Aplicados

| Patrón | Dónde se aplica |
|---|---|
| **State** | Estados de partida (WAITING/SETUP/ACTIVE/FINISHED) y fases del turno (DRAW/MAIN/ATTACK/BETWEEN_TURNS) |
| **Strategy** | Efectos de cada tipo de carta de Entrenador y efectos de cada ataque |
| **Chain of Responsibility** | Pipeline de resolución de ataque (los 7 pasos de RF-01c) |
| **Observer** | Notificación de eventos de juego a clientes vía WebSocket |
| **Repository** | Acceso a datos de cartas, mazos y estado de partida |
| **Facade** | GameEngineFacade: expone una API interna simple, oculta la complejidad del motor |
| **Hexagonal (Ports & Adapters)** | Aislamiento del Game Engine del transporte (REST/WebSocket) y de la persistencia |

El Game Engine debe poder funcionar de forma completamente independiente al transporte: un test puede invocarlo directamente sin Spring, sin HTTP y sin base de datos real.

### RNF-05 — Seguridad

- No usar librerías con vulnerabilidades críticas o altas conocidas (verificar con OWASP Dependency Check)
- **Todas** las acciones de juego se validan en el backend. Las validaciones del frontend son complementarias
- La mano del oponente **nunca** se envía al cliente, solo la cantidad de cartas
- El orden del mazo y el contenido de las cartas de Premio permanecen ocultos hasta que las reglas lo permitan
- Validación de todas las entradas de usuario en el backend
- Manejo adecuado de errores HTTP (400, 401, 403, 404, 409, 500)
- *(Opcional)* Autenticación mediante JWT

### RNF-06 — Usabilidad y Experiencia de Usuario

- Mensajes de error claros y accionables:
  - ❌ `"Error de validación"`
  - ✅ `"No puedes atacar: te falta 1 Energía de Fuego para usar Llamarada"`
- Feedback visual inmediato: botones habilitados/deshabilitados según contexto
- Highlight de targets válidos (Pokémon evolucionables, que pueden recibir energía, etc.)
- Retroalimentación ante cada acción relevante
- Transiciones fluidas entre estados del juego

---