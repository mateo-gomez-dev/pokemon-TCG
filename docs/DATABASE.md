## Modelo de Base de Datos

```sql
-- Jugadores
players (id, username, email, created_at)

-- Cartas cacheadas de pokemontcg.io
cached_cards (
  id VARCHAR PRIMARY KEY,   -- id de la API (ej: "xy1-1")
  set_id VARCHAR,           -- "xy1"
  name VARCHAR,
  supertype VARCHAR,        -- "Pokémon", "Trainer", "Energy"
  subtypes TEXT[],          -- ["Basic"], ["EX"], ["Item"], ["Supporter"], etc.
  hp INTEGER,
  types TEXT[],
  weakness_type VARCHAR,
  resistance_type VARCHAR,
  retreat_cost INTEGER,
  image_url VARCHAR,
  card_data JSONB,          -- datos completos de la API (attacks, abilities, etc.)
  cached_at TIMESTAMP
)

-- Mazos de jugadores
decks (id, player_id FK, name, is_valid, created_at, updated_at)

-- Cartas en mazos
deck_cards (id, deck_id FK, card_id FK, quantity)

-- Partidas
games (id, status VARCHAR, player1_id FK, player2_id FK, current_turn_player_id FK,
       game_state_json JSONB, winner_id FK, created_at, finished_at)
-- game_state_json: snapshot completo del estado tras cada acción

-- Log de acciones (inmutable)
game_logs (id, game_id FK, turn_number INTEGER, player_id FK,
           action_type VARCHAR, action_data JSONB, result JSONB, created_at TIMESTAMP)

-- Cartas de Premio (ocultas hasta revelar)
prize_cards (id, game_id FK, player_id FK, position INTEGER,
             card_instance_id VARCHAR, revealed BOOLEAN)
```

---