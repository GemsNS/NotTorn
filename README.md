# NotTorn — Text-Based Crime Simulation Engine

> A single-player, idle-progression RPG simulation built entirely in Java,
> inspired by the mechanics of the browser MMORPG *Torn City*.
> Features a real-time game loop, dynamic economy, turn-based combat,
> NPC state machines, and a full Lanterna terminal UI.

---

## Table of Contents

- [About the Project](#about-the-project)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Controls](#controls)
- [Data Files](#data-files)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Author](#author)

---

## About the Project

NotTorn is a final-project submission for the Logic & Programming course at NSCC.
The goal was to design and implement a non-trivial, multi-system Java application
from scratch — no tutorials, no frameworks beyond what was specified.

The engine simulates the core gameplay loops of a persistent crime-sandbox game:
regenerating stats, committing crimes, training for combat, managing inventory,
and navigating a living city populated by AI-driven NPCs.

---

## Features

### Phase 1 — Foundation & Game Loop
- Fixed-timestep delta-time game loop running at 60 updates/second
- Lanterna `DefaultTerminalFactory` Swing terminal window
- Decoupled render and logic passes

### Phase 2 — Core Player State & Idle Regeneration
- **Energy** — regenerates 5 every 15 min (standard) / 10 min (donator); natural cap 100, consumable hard cap 1,000
- **Nerve** — regenerates 1 every 5 min; capped by the Natural Nerve Bar (NNB)
- **Happiness** — property-derived baseline; consumables push it to 99,999; hard truncation at exact 15-min clock marks (XX:00 / XX:15 / XX:30 / XX:45)
- **NNB scaling** — step-function increases driven by hidden Crime Experience (CE)
- **Offline Progression** — all missed regen ticks and happiness resets applied on game boot

### Phase 3 — Infinite Scaling & Turn-Based Combat
- Battle stats (Strength, Defense, Speed, Dexterity) stored as `BigInteger` — no overflow ever
- Turn-based combat up to 25 turns between any two combatants
- **Hit chance** — logarithmic table interpolation over empirical Speed vs. Dexterity benchmarks (50% at equal stats)
- **Damage mitigation** — logarithmic table interpolation over Defense vs. Strength benchmarks (50% at equal stats)
- 12% base critical hit chance (merit-expandable)
- 14 weighted hitboxes; criticals to Head/Throat/Heart apply a 3.5× multiplier
- Merit level system with per-stat percentage bonuses

### Phase 4 — Dynamic Economy & Inventory
- 20 items across Weapon, Armor, and Consumable categories loaded from `items.json`
- Supply-and-demand pricing: `price = basePrice × (refSupply / currentSupply) ^ elasticity`
- 5% sales tax on all sell-back transactions
- 10,000-item per-transaction cap
- Persistent shop state saved to `data/shop_state.json`

### Phase 5 — NPC State Machines & The World Map
- 8-location city graph loaded from `world.json` (City Center, Gym, Alley, Market, etc.)
- 4 live NPCs loaded from `npcs.json` — each with HP, battle stats, and patrol interval
- FSM: `IDLE → PATROL → CHASE → COMBAT → DEAD` with configurable timers and respawn
- Tutorial NPC **George** (indestructible) delivers 5 ordered missions loaded from `missions.json`
- Silent mission completion — objectives tracked automatically as player visits locations

### Phase 6 — Progression Modules (Jobs, Crimes, Travel)
- **City Jobs** — 4 career paths (Army, Medical, Education, Law) with 3–6 ranks each; daily stat gains and income award applied in real time; auto-promotion when stats qualify; offline ticks catch up on boot
- **Crimes** — 10 crime types from Shoplifting (1 nerve) to Kidnapping (20 nerve); success formula modified by CE bonus; failures award CE penalties and optional jail time
- **Travel** — 11 international destinations with exact spec flight times (Mexico 26 min → South Africa 297 min); full travel lockout while in flight; return-home mechanic

### Phase 7 — Serialization & Polish
- Full Jackson save/load roundtrip for `Player` → `data/savegame.json` on every auto-save (60 s) and clean exit
- Shop supply state persisted to `data/shop_state.json` separately
- Three full-screen UI panels: **Inventory** (`I`), **City Map** (`M`), **Combat Log** (`L`)
- In-game shop: numbered **Buy** (`B`) and **Sell** (`S`) menus with live prices and tax preview
- Title bar, status bar with hotkey hints, jail/travel timers, and active mission objective

---

## Architecture

```
nottorn/
├── Main.java                   Entry point — wires all services
├── engine/
│   ├── GameEngine.java         Fixed-timestep loop, input routing, menu dispatch
│   ├── GameServices.java       Service bundle (Phase 5–7)
│   ├── RegenerationEngine.java Energy / nerve / happiness ticks
│   └── OfflineProgressionService.java  Catch-up ticks on boot
├── model/
│   ├── Player.java             Central player POJO (all fields serialised)
│   ├── GameConfig.java         game_config.json binding
│   ├── Inventory.java          Map<itemId, quantity>
│   └── item/                   Item hierarchy (Item, Weapon, Armor, Consumable)
├── combat/
│   ├── CombatEngine.java       Turn-based fight orchestrator
│   ├── CombatCalculator.java   Hit-chance & mitigation via log-table interpolation
│   ├── CharacterStats.java     BigInteger stats + merit multipliers
│   ├── Hitbox.java             14 weighted body regions
│   ├── CombatLog.java          Immutable fight record
│   └── CombatOutcome.java      Post-victory choices
├── economy/
│   ├── Shop.java               Supply-demand market
│   ├── ShopEntry.java          Per-item live price/supply state
│   └── ItemCatalog.java        items.json loader
├── world/
│   ├── WorldMap.java           Location graph + NPC container
│   ├── Location.java           Graph node
│   ├── Npc.java                Live NPC instance
│   ├── NpcBehaviour.java       Stateless FSM driver
│   ├── NpcState.java           FSM state enum
│   ├── NpcTemplate.java        npcs.json binding
│   ├── TutorialNpc.java        George — mission chain logic
│   └── TutorialMission.java    missions.json binding
├── jobs/
│   ├── JobService.java         Daily tick, enrollment, promotion
│   ├── JobCatalog.java         jobs.json loader
│   ├── JobCareer.java          Career POJO
│   └── JobRank.java            Rank POJO
├── crime/
│   ├── CrimeService.java       Success roll, CE logic, jail
│   ├── CrimeCatalog.java       crimes.json loader
│   └── CrimeDefinition.java    Crime POJO
├── travel/
│   ├── TravelService.java      Depart / arrive / return-home
│   ├── TravelCatalog.java      destinations.json loader
│   └── Destination.java        Destination POJO
├── persistence/
│   └── SaveManager.java        Jackson load/save for Player and Shop
└── ui/
    └── Renderer.java           All Lanterna draw calls; menu + panel system
```

### Key Design Decisions

| Concern | Decision |
|---|---|
| Infinite stat scaling | `BigInteger` for base stats, `BigDecimal` for intermediate combat maths |
| Combat formulas | Table-based log interpolation — exact match to all spec benchmarks |
| Data-driven design | Every item, NPC, job, crime, and destination lives in a `.json` file |
| No build tool | JARs placed in `lib/`; classpath set in `.vscode/settings.json` |
| Persistence | Jackson `ObjectMapper` with `INDENT_OUTPUT`; `@JsonIgnoreProperties(ignoreUnknown=true)` allows safe schema evolution |

---

## Getting Started

### Prerequisites
- Java SDK 17 or later
- The three JARs in `lib/` (already included):
  - `lanterna-3.1.2.jar`
  - `jackson-databind-2.16.1.jar`
  - `jackson-core-2.16.1.jar`
  - `jackson-annotations-2.16.1.jar`

### Compile

```powershell
# From the project root (d:\...\final)
javac --release 17 -cp "lib/*" -d bin `
    (Get-ChildItem -Recurse src -Filter "*.java").FullName
```

Or on Linux/macOS:

```bash
find src -name "*.java" | xargs javac --release 17 -cp "lib/*" -d bin
```

### Run

```powershell
java -cp "lib/*;bin" nottorn.Main
```

```bash
java -cp "lib/*:bin" nottorn.Main
```

A Swing terminal window will open. The game saves automatically every 60 seconds
and on clean exit to `data/savegame.json`.

---

## Controls

| Key | Action |
|---|---|
| `C` | Open crime selection menu |
| `J` | Open job enrollment / resign menu |
| `T` | Open travel departure menu |
| `G` | Talk to George (tutorial missions) |
| `R` | Return home from abroad |
| `B` | Buy from Item Market |
| `S` | Sell items from inventory |
| `I` | Toggle Inventory panel |
| `M` | Toggle City Map panel |
| `L` | Toggle last Combat Log panel |
| `1`–`9` | Select numbered menu item |
| `0` / `ESC` | Close menu or panel / quit game |

---

## Data Files

All game data lives under `data/` and can be edited without recompiling.

| File | Purpose |
|---|---|
| `game_config.json` | Regen intervals, caps, combat constants, shop elasticity |
| `items.json` | 20 item definitions (Weapon / Armor / Consumable) |
| `world.json` | 8 city locations with adjacency connections |
| `npcs.json` | 4 NPC templates with stats and FSM configuration |
| `missions.json` | 5 ordered tutorial missions |
| `jobs.json` | 4 career paths with rank requirements and daily gains |
| `crimes.json` | 10 crime definitions with nerve costs and reward ranges |
| `destinations.json` | 11 travel destinations with exact flight times |
| `savegame.json` | *(generated)* Player save state |
| `shop_state.json` | *(generated)* Live shop supply levels |

---

## Dependencies

| Library | Version | Use |
|---|---|---|
| [Lanterna](https://github.com/mabe02/lanterna) | 3.1.2 | Terminal emulation in a Swing frame |
| [Jackson Databind](https://github.com/FasterXML/jackson-databind) | 2.16.1 | JSON serialisation / deserialisation |

---

## Author

**Joel** — Student at NSCC (Nova Scotia Community College)

> *Final project submission — Logic & Programming*

- Portfolio: [gemsns.github.io](https://gemsns.github.io)
- GitHub: [github.com/gemsns](https://github.com/gemsns)

---

*NotTorn is a fan-inspired academic project. It is not affiliated with or endorsed by the creators of Torn City.*
