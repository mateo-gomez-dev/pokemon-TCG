# 🎯 Features Adicionales (Extras del Proyecto)

Este documento define posibles extensiones del sistema para mejorar la calidad técnica, experiencia de usuario y puntaje del proyecto.
Cada feature está descrita con su objetivo, alcance y consideraciones de implementación.

---

## 🧩 F-01 — Implementación de Requerimientos Opcionales

**Tipo:** Funcional
**Prioridad:** Alta

### Objetivo

Completar todos los requerimientos opcionales definidos en el enunciado del proyecto.

### Alcance

* Chat entre jugadores
* Sistema de ranking / historial de partidas
* Animaciones (si aplica)

### Consideraciones

* No modifican la lógica central del juego
* Se apoyan en el sistema existente (WebSockets, persistencia)

---

## 🃏 F-02 — Expansión de Cartas (Múltiples Sets)

**Tipo:** Funcional
**Prioridad:** Media

### Objetivo

Permitir el uso de múltiples sets de cartas además de `xy1`.

### Alcance

* Soporte para nuevos sets (ej: expansiones adicionales)
* Adaptación del Deck Builder para múltiples sets
* Cache local extendido de cartas

### Consideraciones

* El Game Engine debe ser independiente del set
* No debe requerir cambios en reglas del juego
* Validar compatibilidad entre sets (opcional)

---

## 🎵 F-03 — Sistema de Música Dinámica

**Tipo:** No Funcional (UX)
**Prioridad:** Baja

### Objetivo

Mejorar la experiencia del usuario mediante música contextual.

### Alcance

* Música en menú principal
* Música durante partida (battle theme)
* Cambio automático según estado (menu → partida)

### Consideraciones

* Implementado únicamente en frontend
* No afecta lógica del juego
* Debe poder activarse/desactivarse

---

## 🤖 F-04 — Modo Usuario vs IA

**Tipo:** Funcional
**Prioridad:** Muy Alta

### Objetivo

Permitir jugar contra una IA controlada por el sistema.

### Alcance

* IA como jugador válido dentro del Game Engine
* Uso de acciones válidas generadas por el sistema
* Integración con flujo normal de partida

### Consideraciones

* La IA NO implementa reglas, usa el Game Engine
* Debe poder tomar decisiones basadas en el estado actual

---

## 🧠 F-05 — Sistema de Dificultad de IA

**Tipo:** Funcional
**Prioridad:** Alta

### Objetivo

Ofrecer distintos niveles de dificultad para la IA.

### Niveles

#### Fácil

* Selección aleatoria de acciones válidas

#### Medio

* Priorización de acciones:

  * Atacar > Evolucionar > Energía > Trainer

#### Difícil

* Evaluación de acciones mediante scoring:

  * KO → prioridad máxima
  * Mayor daño → prioridad alta
  * Mejor posicionamiento → prioridad media

### Consideraciones

* Implementado con Strategy Pattern
* No debe modificar el Game Engine

---

## ⚙️ F-06 — Modo Automático (Auto-Play)

**Tipo:** Funcional
**Prioridad:** Alta

### Objetivo

Permitir que la IA tome control de un jugador en cualquier momento.

### Alcance

* Activación/desactivación durante partida
* Uso del mismo sistema de IA
* Posibilidad de IA vs IA

### Consideraciones

* Reutiliza F-04 (IA)
* Ideal para testing y simulación

---

## 🏆 F-07 — Sistema de Partidas Competitivas (MMR)

**Tipo:** Funcional
**Prioridad:** Media

### Objetivo

Separar partidas en modos casual y competitivo.

### Alcance

* Sistema de ranking (MMR)
* Matchmaking basado en nivel
* Historial de partidas

### Consideraciones

* Persistencia de estadísticas
* No afecta reglas del juego

---

## 👥 F-08 — Sistema de Amigos

**Tipo:** Funcional
**Prioridad:** Media

### Objetivo

Permitir interacción directa entre jugadores.

### Alcance

* Agregar/eliminar amigos
* Lista de usuarios conectados
* Invitar a partidas

### Consideraciones

* Requiere sistema de usuarios
* Integración con matchmaking

---

## ⭐ F-09 — Favoritos y Filtros de Cartas

**Tipo:** Funcional
**Prioridad:** Baja

### Objetivo

Mejorar la navegación y personalización del Deck Builder.

### Alcance

* Marcar cartas como favoritas
* Filtros por:

  * Tipo
  * Energía
  * Número de carta
* Ordenamiento personalizado

### Consideraciones

* Impacto solo en frontend / experiencia
* Persistencia opcional por usuario

---

# 🧠 Notas Generales

* Todas las features deben respetar:

  * Backend como fuente de verdad
  * Separación de responsabilidades
  * Desacople del Game Engine

* Las features de IA deben:

  * Utilizar acciones válidas del sistema
  * Nunca modificar reglas del juego

* Las mejoras de UX:

  * No deben afectar la lógica de negocio
  * Deben ser opcionales cuando sea posible

---
