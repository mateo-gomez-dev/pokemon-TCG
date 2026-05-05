## Estrategia de Testing

### Filosofía TDD para el Game Engine

```
Paso 1 — Escribir el test (RED)
  └── El test describe el comportamiento esperado en lenguaje de negocio
  └── El test compila pero falla porque la implementación no existe

Paso 2 — Implementación mínima (GREEN)
  └── Escribir el mínimo código necesario para que el test pase
  └── No anticipar funcionalidad futura

Paso 3 — Refactor
  └── Limpiar el código sin romper los tests
  └── Aplicar patrones de diseño si corresponde
```

### Ejemplos de Tests por Componente

```java
// DamageCalculatorTest.java
@Test
void deberia_aplicar_debilidad_multiplicando_por_dos() {
    // Given
    PokemonCard atacante = buildPokemon(type: FIRE);
    PokemonCard defensor = buildPokemon(weaknessType: FIRE);
    int danioBase = 60;

    // When
    int resultado = calculator.calculate(danioBase, atacante, defensor, context);

    // Then
    assertThat(resultado).isEqualTo(120); // 60 × 2
}

@Test
void deberia_aplicar_resistencia_restando_20_con_minimo_cero() {
    // ...
}

// StatusEffectManagerTest.java
@Test
void dormido_confundido_paralizado_son_mutuamente_excluyentes() {
    // Si un Pokémon está Dormido y se le aplica Confusión,
    // la Confusión reemplaza al Sueño
}

@Test
void orden_de_procesamiento_entre_turnos_es_fijo() {
    // Envenenado → Quemado → Dormido → Paralizado
}

// RuleValidatorTest.java
@Test
void no_puede_atacar_en_el_primer_turno_del_jugador_que_empieza() { ... }

@Test
void no_puede_evolucionar_en_el_turno_que_el_pokemon_entro_en_juego() { ... }
```

### Tests de Integración

```java
// FullGameIntegrationTest.java
@SpringBootTest
class FullGameIntegrationTest {
    @Test
    void partida_completa_desde_setup_hasta_victoria_por_premios() {
        // Crear 2 mazos válidos → setup → turnos → KO → tomar premios → victoria
    }
}
```

---