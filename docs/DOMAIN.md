## 1. Dominio del Juego — Modelo Conceptual

### 1.1 Tipos de Cartas

| Tipo | Subtipo | Reglas Clave |
|---|---|---|
| **Pokémon Básico** | — | Se coloca directamente en Banca o como Activo |
| **Pokémon Fase 1** | Evolución | Evoluciona desde un Básico. No en el primer turno del Pokémon ni en el primer turno del jugador |
| **Pokémon Fase 2** | Evolución | Evoluciona desde una Fase 1. Mismas restricciones |
| **Pokémon-EX** | Básico especial | Al quedar KO, el oponente toma **2 cartas de Premio** |
| **Pokémon Mega** | *(Opcional)* | Evoluciona de un EX. Al jugarla, **el turno termina inmediatamente**. KO = 2 premios |
| **Energía Básica** | — | Sin límite de copias en el mazo. 1 por turno |
| **Energía Especial** | — | Máximo **4 copias** en el mazo. Efecto especial según carta |
| **Entrenador – Objeto** | Item | Ilimitados por turno. Efecto inmediato, se descarta |
| **Entrenador – AS TÁCTICO** | Ace Spec | Subtipo de Objeto. Solo **1 en todo el mazo**, sin importar cuál |
| **Entrenador – Partidario** | Supporter | Máximo **1 por turno**. Efecto inmediato, se descarta |
| **Entrenador – Estadio** | Stadium | Máximo **1 por turno**. Permanece en zona compartida. Reemplaza al anterior |
| **Entrenador – Herramienta** | Tool | Se une a un Pokémon. Máximo **1 por Pokémon**. Permanece hasta que el Pokémon sea descartado |

### 1.2 Condiciones Especiales

| Condición | Símbolo | Efecto entre turnos | Exclusividad |
|---|---|---|---|
| **Dormido** | 💤 | Lanzar moneda: cara → despierta; cruz → sigue dormido | Mutuamente excluyente con Confundido y Paralizado |
| **Confundido** | 🌀 | Al atacar: lanzar moneda; cruz → ataque falla + 3 contadores de daño al atacante | Mutuamente excluyente con Dormido y Paralizado |
| **Paralizado** | ⚡ | No puede atacar ni retirarse. Se cura automáticamente al final del turno en que fue aplicado | Mutuamente excluyente con Dormido y Confundido |
| **Envenenado** | 💜 | 1 contador de daño automáticamente (sin moneda) | Compatible con todos |
| **Quemado** | 🔥 | Lanzar moneda: cruz → 2 contadores de daño | Compatible con todos |

**Orden de procesamiento entre turnos (fijo e inamovible):**

```
(1) Envenenado  →  (2) Quemado  →  (3) Dormido  →  (4) Paralizado
→  Verificar Habilidades  →  Verificar KO
```

**Eliminación:** Todas las condiciones especiales se eliminan cuando el Pokémon **se retira a la Banca** o **evoluciona**.

### 1.3 Estructura de un Turno

```
DRAW Phase
  └── Robar 1 carta del mazo (excepto el jugador que va primero en su primer turno)
  └── Si el mazo está vacío al robar → ese jugador PIERDE

MAIN Phase (acciones opcionales, en cualquier orden)
  ├── Colocar Pokémon Básico en Banca (hasta tener 5 en Banca)
  ├── Evolucionar Pokémon (restricciones: no en turno que entró, no en primer turno)
  ├── Unir 1 Energía a cualquier Pokémon propio (1 vez por turno)
  ├── Jugar cartas de Entrenador:
  │     ├── Objetos: ilimitados
  │     ├── Partidario: máximo 1 por turno
  │     └── Estadio: máximo 1 por turno
  ├── Retirar el Pokémon Activo (1 vez por turno, pagando el costo de retirada)
  └── Usar Habilidades de Pokémon propios

ATTACK Phase (opcional — NO disponible en el primer turno del jugador que empieza)
  └── Ver Sección 5.4 — Pipeline de Resolución de Ataque
  └── El ataque FINALIZA el turno automáticamente

BETWEEN_TURNS Phase (automático — procesado por el servidor)
  └── Ver Sección 5.2 — Orden de condiciones especiales
  └── Verificar KO
  └── Turno pasa al jugador contrario → vuelve a DRAW
```

### 1.4 Pipeline de Resolución de Ataque (7 Pasos)

Implementado con el patrón **Chain of Responsibility**. Cada paso puede interrumpir la cadena.

```
Paso 1: EnergyValidationStep
  └── Verificar que el Pokémon Activo tenga las Energías requeridas para el ataque
  └── Si falta energía → ERROR, ataque inválido

Paso 2: ConfusionCheckStep
  └── Si el Pokémon Activo está Confundido → lanzar moneda
  └── Cruz → ataque falla, atacante recibe 3 contadores de daño, turno termina
  └── Cara → continúa

Paso 3: SelectionStep
  └── Resolver selecciones requeridas por el ataque (elegir objetivo, etc.)

Paso 4: PreAttackStep
  └── Ejecutar requisitos previos (lanzamientos de moneda indicados en el texto)

Paso 5: ModifierStep
  └── Aplicar efectos que modifican o cancelan el ataque (efectos del rival, etc.)

Paso 6: DamageApplicationStep
  └── Daño base (indicado en la carta)
  └── + Modificadores por efectos sobre el atacante
  └── × 2 si el defensor tiene Debilidad al tipo del atacante
  └── − 20 si el defensor tiene Resistencia al tipo del atacante (mínimo: 0)
  └── + Modificadores por efectos sobre el defensor
  └── Colocar 1 contador de daño por cada 10 puntos resultantes

Paso 7: PostDamageEffectStep
  └── Aplicar condiciones especiales infligidas
  └── Descartar Energías si el ataque lo requiere
  └── Daño a Pokémon en Banca si el ataque lo indica
  └── Curación si el ataque lo indica
  └── Verificar KO
```

### 1.5 Proceso de Preparación (Setup)

```
1. Ambos jugadores barajan sus mazos (60 cartas)
2. Ambos roban 7 cartas iniciales
3. Mulligan check: ¿Hay al menos 1 Pokémon Básico en la mano?
   └── NO → Mostrar mano al rival, barajar, volver a robar 7
             El RIVAL puede robar 1 carta adicional por cada mulligan declarado
             Repetir hasta que ambos tengan al menos 1 Básico
4. Cada jugador coloca su Pokémon Activo boca abajo
5. Cada jugador coloca hasta 5 Pokémon Básicos en Banca, boca abajo
6. Cada jugador toma las primeras 6 cartas de su mazo → cartas de Premio (boca abajo)
7. Lanzar moneda para determinar quién empieza
8. Ambos jugadores revelan sus Pokémon → comienza la partida
```

### 1.6 Validaciones del Mazo (Deck Builder)

| Regla | Valor |
|---|---|
| Total de cartas | Exactamente **60** |
| Copias del mismo nombre | Máximo **4** (excepto Energía Básica: sin límite) |
| AS TÁCTICO | Máximo **1** en todo el mazo (cualquier carta de ese subtipo) |
| Pokémon Básico | Al menos **1** |
| Set base | Exclusivamente cartas del set **xy1** (XY Unlimited) |

---