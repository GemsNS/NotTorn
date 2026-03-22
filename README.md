# NotTorn — Text-Based Crime Simulation

> A single-player idle-progression RPG simulation engine written in Java.
> Navigate a living city, commit crimes, train for combat, manage a dynamic
> economy, and outlast AI-driven enemies in turn-based fights — all inside
> a retro Swing terminal window.

---

**Joel — Student at NSCC**
[gemsns.github.io](https://gemsns.github.io) &nbsp;·&nbsp; [github.com/gemsns](https://github.com/gemsns) &nbsp;·&nbsp;  [Torn User: novascotia [3983589]](https://www.torn.com/profiles.php?XID=3983589)

---

## Table of Contents

- [Screenshots](#screenshots)
- [Bug Fixes](#bug-fixes)
- [Game Features](#game-features)
- [Technical Overview](#technical-overview)
- [Mathematics & Weights](#mathematics--weights)
- [Data Files](#data-files)
- [Getting Started](#getting-started)
- [Controls](#controls)
- [Roadmap / TODO](#roadmap--todo)

---

## Screenshots

| | |
|:---:|:---:|
| ![Main UI — city overview and stats panel](https://i.imgur.com/v7j3JDr.png) | ![Crime menu](https://i.imgur.com/pbKQyFs.png) |
| **Main UI — city overview and stats panel** | **Crime menu** |
| ![Combat / turn-based fight](https://i.imgur.com/lPT2pDT.png) | ![City map panel](https://i.imgur.com/VlqIGtz.png) |
| **Combat — turn-based fight** | **City map panel** |

---

## Bug Fixes

### Mar 2026 — Flickering, Mexico Stuck & Terminal Resize (v0.6.1 → v0.6.2)

---

#### Bug 1 — Screen flickering (all platforms)

**Symptom:** The terminal window flickered constantly during normal gameplay.

**Root cause:** `render()` was called on every iteration of the game loop with only
`Thread.sleep(1)` between iterations — up to ~1,000 `screen.refresh()` calls per
second. The Swing AWT paint thread on Windows, and the ANSI escape-code writer on
Linux, cannot keep up with that volume and draw partial frames.

**Fix (`GameEngine.java`):** Added `RENDER_INTERVAL_NS` (60 fps cap) and a
`lastRenderNs` timestamp. `render()` only fires when a full frame interval has
elapsed. Logic updates and input handling are unaffected — they still run on the
existing 60 Hz fixed-timestep independently of the render cap.

---

#### Bug 2 — Flickering persisted after the 60 fps cap (all platforms)

**Symptom:** Flickering continued even after the render-rate cap, on all platforms.
Most visible on GPU-accelerated terminal emulators (Alacritty, Kitty, WezTerm, foot)
but present on Windows and standard Linux terminals too.

**Root cause:** `screen.clear()` was called at the top of every render pass. This
marks the entire back buffer as dirty, causing `RefreshType.AUTOMATIC` to choose a
full repaint on every frame — the mechanism differs by platform but the result is the
same:

- **Linux / macOS (ANSI terminals):** Lanterna sends `\033[2J` (erase-display) to the
  terminal before writing the new frame. The screen blanks instantly while content is
  drawn cell-by-cell — that gap is the visible flash.
- **Windows (Swing terminal):** The full-dirty back buffer causes Lanterna to repaint
  every cell in the Swing component every frame, flooding the AWT Event Dispatch Thread
  with redundant work and producing the same tearing effect.

The 60 fps cap reduced how *often* the flash occurred but could not remove it because
the cause was in the render call itself, not just the call frequency.

**Fix (`Renderer.java`):**
- Removed `screen.clear()`. `fillBackground()` already writes a space with the
  background colour to every cell, so the visual result is identical — there is no
  intermediate blank or dirty state for the platform to act on.
- Both `screen.refresh()` calls changed to `Screen.RefreshType.DELTA`. This locks
  Lanterna into incremental diff mode on every platform: only cells whose character
  or colour actually changed since the last frame are updated. On a typical idle frame
  (only timer digits changed) that is a handful of characters instead of 4,800.

> **Note — synchronized output mode:** For GPU-accelerated terminals on Linux the
> ideal further fix is wrapping each refresh in DEC private mode 2026
> (`\033[?2026h` … `\033[?2026l`), which instructs the GPU compositor to hold
> rendering until the end marker arrives. All major GPU-accelerated Linux terminals
> support this. It requires writing raw escape codes to Lanterna's internal output
> stream, which is not exposed through the `Screen` or `Terminal` interfaces in
> version 3.1.2 and is left as a future improvement.

---

#### Bug 3 — Window resize does not redraw the game (all platforms)

**Symptom:** Resizing the terminal window left the game frozen at the old size.
Content outside the original frame boundary was not cleared or redrawn.

**Root cause:** Lanterna queues resize events internally — via `SIGWINCH` on
Linux/macOS, and via Swing component events on Windows — but does not apply them
until `screen.doResizeIfNecessary()` is explicitly called. Without that call,
`screen.getTerminalSize()` returns stale dimensions, Lanterna's virtual front/back
buffers remain the wrong size, and any area outside the old frame is never written
to — leaving frozen content after a resize. This affects every platform.

**Fix (`Renderer.java`):** `screen.doResizeIfNecessary()` is called at the top of
every render pass. If it returns non-null (a resize occurred), the refresh type for
that frame switches to `Screen.RefreshType.COMPLETE`, pushing every cell at the new
dimensions out to the terminal and clearing any stale content. All subsequent frames
return to `DELTA`. The one-frame `COMPLETE` flash during an active resize is expected
behaviour and imperceptible in practice.

---

#### Additional hardening

| Change | File | Reason |
|---|---|---|
| `data/savegame.json` deleted | — | Committed dev save had `currentLocationId: MEXICO` and a jail timestamp expiring 2027; every fresh clone inherited it and started broken |
| `.gitignore` added | `.gitignore` | Prevents `savegame.json` and `shop_state.json` from being committed again |
| Travel menu prepends *"Return to Torn City"* when abroad | `GameEngine.java` | Players could not discover the `R` shortcut; the menu now makes the return path explicit |
| Corrupt `IN_FLIGHT + traveling=false` state healed on load | `Main.java` | A mid-flight crash left players permanently stuck on the IN_FLIGHT screen — `checkArrival()` requires `traveling=true` to fire |
| Arrival message explains how to return | `TravelService.java` | Foreign arrival now reads *"Press [T] or [R] to book your return flight"*; home arrival gives a distinct *"Welcome home!"* |

---

## Game Features

### The City
The player wakes up in **City Center** with a small amount of cash and an empty inventory.
The city is a network of eight connected districts, each with its own character:

| District | What happens there |
|---|---|
| City Center | Central hub; George the tutorial NPC lives here |
| Gym | Train battle stats (future phase) |
| Hospital | Recover HP; medical items available |
| Drug Store | Consumables, medical supplies |
| Item Market | Buy and sell weapons, armor, and goods |
| Dark Alley | Crimes happen here; hostile NPCs patrol |
| Police Station | Officers patrol; busting jailed players (future) |
| Bank | Deposit cash; investment accounts (future) |

Moving between districts happens instantly. Your current location and its
exits are always visible on the left panel. Press **M** to open a full ASCII city map.

---

### Vital Stats
Three volatile stats govern the pace of play and regenerate in real time:

**Energy** — used for gym training and initiating combat.
Regenerates continuously. Consumables like Xanax can push it far above its
natural ceiling up to a hard cap of 1,000.

**Nerve** — spent to commit crimes.
Its ceiling (the *Natural Nerve Bar*, NNB) grows as you accumulate **Crime
Experience (CE)**, unlocking more dangerous and lucrative crime options over time.

**Happiness** — a passive multiplier for gym gains (future).
Can be stacked to 99,999 with consumables, but the engine enforces a hard
server-style truncation back to your property baseline precisely at every
15-minute clock mark (XX:00, XX:15, XX:30, XX:45).

All three stats regenerate while the game is closed. The next time you boot
the engine it calculates every tick you missed and awards them instantly.

---

### Crime
Press **C** to open the crime menu. Ten crime types are available, ranging from
petty shoplifting to kidnapping:

- Each crime costs **Nerve** to attempt.
- A successful attempt awards **cash** and **Crime Experience**.
- Failure deducts CE and may send you to jail for a timed lockout.
- Your success rate improves as your CE total grows — experienced criminals
  are better at their trade.

---

### Combat
Any hostile NPC that spots you in its district will patrol toward you,
and once it reaches you, a fight triggers automatically.

Fights are turn-based, capped at 25 rounds. Each turn:
1. The attacker rolls for a **hit** based on Speed vs. the defender's Dexterity.
2. If it hits, a weighted random **hitbox** is selected from 14 body regions.
3. A **critical hit** roll (12% base chance) determines the damage multiplier.
4. The defender's **Defense** absorbs a portion of the attacker's **Strength**-based damage.

Winning kills the NPC (it respawns after a cooldown). Losing costs you HP —
the engine never kills you outright so you can always recover.
Press **L** after a fight to read the full turn-by-turn combat log.

Your four **Battle Stats** (Strength, Defense, Speed, Dexterity) are stored
as `BigInteger` — they scale infinitely and will never overflow.

---

### Jobs
Press **J** to view available careers. Four paths exist, each with multiple
ranks unlocked by reaching working-stat thresholds:

| Career | Top rank | Top daily income |
|---|---|---|
| Army | General | $2,500 |
| Medical | Brain Surgeon | $7,000 |
| Education | Principal | $3,250 |
| Law | Federal Judge | $5,000 |

Once employed, your working stats (Manual Labor, Intelligence, Endurance)
and income are awarded automatically every 24 real-world hours. If you were
offline for several days, every missed tick is applied on next boot.
When your stats surpass the next rank's requirements, you are automatically promoted.

---

### Economy & Inventory
Press **B** to buy and **S** to sell at the Item Market. Prices are not fixed —
they respond to player activity in real time. A 5% sales tax applies on all sell-backs.

Press **I** to view your inventory with current market prices per item.

---

### International Travel
Press **T** to board a flight to one of 11 destinations. While in the air you
are locked out of crimes and combat — the engine tracks your arrival timestamp
and notifies you the moment you land.

| Destination | Flight time | Ticket cost |
|---|---|---|
| Mexico | 26 min | $6,500 |
| Cayman Islands | 35 min | $10,000 |
| Canada | 41 min | $9,000 |
| Hawaii | 2 h 14 min | $11,000 |
| United Kingdom | 2 h 39 min | $18,000 |
| Argentina | 2 h 47 min | $21,000 |
| Switzerland | 2 h 55 min | $27,000 |
| Japan | 3 h 45 min | $32,000 |
| China | 4 h 2 min | $35,000 |
| UAE | 4 h 31 min | $32,000 |
| South Africa | 4 h 57 min | $40,000 |

Press **R** from abroad to return home on the same flight time.

---

### Tutorial — George
George the Fragrant Vagrant stands in City Center and guides new players
through five introductory missions. Press **G** to talk to him. Each mission
rewards cash on completion and is tracked silently — if you visit the
objective location before talking to George, the reward triggers automatically.

---

## Technical Overview

### Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI | Lanterna 3.1.2 (Swing terminal emulator) |
| Serialisation | Jackson 2.16.1 (`ObjectMapper`) |
| Build | Manual classpath (`lib/*.jar`) |

### Architecture

```
nottorn/
├── Main.java                        Boot, wires all services
├── engine/
│   ├── GameEngine.java              60 Hz fixed-timestep loop; input routing
│   ├── GameServices.java            Service bundle passed to the engine
│   ├── RegenerationEngine.java      Energy / nerve / happiness per-tick logic
│   └── OfflineProgressionService.java  Catch-up ticks on load
├── model/
│   ├── Player.java                  Central POJO; every field serialised to JSON
│   ├── GameConfig.java              game_config.json binding
│   ├── Inventory.java               Map<itemId, quantity>
│   └── item/                        Item hierarchy (Weapon, Armor, Consumable)
├── combat/
│   ├── CombatEngine.java            Turn orchestrator
│   ├── CombatCalculator.java        Hit-chance & mitigation (log-table)
│   ├── CharacterStats.java          BigInteger stats + merit bonuses
│   ├── Hitbox.java                  14 weighted body regions
│   └── CombatLog.java               Immutable fight record
├── economy/
│   ├── Shop.java                    Supply-demand market
│   ├── ShopEntry.java               Per-item live price state
│   └── ItemCatalog.java             items.json loader
├── world/
│   ├── WorldMap.java                Location graph + NPC container
│   ├── NpcBehaviour.java            Stateless FSM driver
│   └── TutorialNpc.java             George — mission chain logic
├── jobs/                            Career paths, daily tick, auto-promotion
├── crime/                           Crime definitions, success roll, jail logic
├── travel/                          Depart / arrive / return-home state machine
├── persistence/
│   └── SaveManager.java             Jackson load/save for Player and Shop
└── ui/
    └── Renderer.java                All Lanterna draw calls; menu + panel system
```

### Game Loop

The engine runs at **60 logical updates per second** using a fixed-timestep
delta-time architecture. Logic updates are completely decoupled from rendering:

```
while (running) {
    elapsed = now - previousTime
    lag    += elapsed

    handleInput()

    while (lag >= UPDATE_RATE_NS) {        // 16.67 ms per tick
        update(deltaSeconds)               // regen, NPC FSMs, travel, job ticks
        lag -= UPDATE_RATE_NS
    }

    if (now - lastRenderNs >= RENDER_INTERVAL_NS) {   // 60 fps cap
        render()                                       // Lanterna screen refresh
        lastRenderNs = now
    }
}
```

### Persistence

`Player` is serialised to `data/savegame.json` every 60 seconds and on clean exit.
`@JsonIgnoreProperties(ignoreUnknown = true)` on every POJO means old save files
load safely when new fields are added — unrecognised keys are silently dropped.
Shop supply state is saved separately to `data/shop_state.json`.

---

## Mathematics & Weights

### Energy & Nerve Regeneration

| Stat | Tick amount | Tick interval (standard) | Tick interval (donator) |
|---|---|---|---|
| Energy | +5 | 15 minutes | 10 minutes |
| Nerve | +1 | 5 minutes | 5 minutes |

Regen **pauses** when the stat is at or above its natural maximum.
Consumables can push energy above the natural cap up to an absolute ceiling of 1,000.

### Natural Nerve Bar (NNB) Step Function

NNB increases in increments of 5 based on accumulated Crime Experience:

| CE threshold | NNB |
|---|---|
| 0 | 10 |
| 100 | 15 |
| 300 | 20 |
| 600 | 25 |
| 1,000 | 30 |
| 1,500 | 35 |
| 2,500 | 40 |
| 4,000 | 45 |
| 6,000 | 50 |
| 9,000 | 55 |
| 13,000 | 60 *(natural cap)* |

### Hit Chance Formula

Hit chance is resolved by comparing the attacker's **Speed** against the
defender's **Dexterity** using logarithmic table interpolation.
The table is anchored to empirical benchmarks with the defender fixed at 10,000,000:

| Attacker Speed | Hit Chance |
|---|---|
| 156,250 | 0.00% |
| 1,000,000 | 10.93% |
| 5,000,000 | 33.26% |
| **10,000,000** | **50.00%** ← equal-stats baseline |
| 20,000,000 | 66.74% |
| 30,000,000 | 74.15% |
| 50,000,000 | 81.59% |
| 100,000,000 | 89.07% |
| 640,000,000 | 100.00% |

The implementation uses **log-linear interpolation** over a pre-computed `HIT_TABLE`
of `(log(speed/dexterity), hitChance)` pairs. Values between anchor points are
linearly interpolated in log space; results are clamped to [0, 1].

### Damage Mitigation Formula

Mitigation is resolved by comparing the defender's **Defense** against the
attacker's **Strength** using the same interpolation approach:

| Defender Defense | Mitigation |
|---|---|
| 312,500 | 0.00% |
| 1,250,000 | 20.00% |
| 5,000,000 | 40.00% |
| **10,000,000** | **50.00%** ← equal-stats baseline |
| 20,000,000 | 63.14% |
| 30,000,000 | 70.81% |
| 50,000,000 | 80.49% |
| 100,000,000 | 93.63% |
| 140,000,000 | 100.00% |

### Critical Hit System

- Base crit chance: **12%** (configurable in `game_config.json`; merit-expandable to +5%)
- On a crit, the targeted hitbox determines the multiplier:

| Zone | Hitboxes | Damage multiplier |
|---|---|---|
| Vital | Head, Throat, Heart | **3.5×** |
| Secondary | Chest, Stomach | **2.0×** |
| Standard | Arms, Legs | **1.0×** |
| Extremity | Hands, Feet | **0.7×** |

- Non-critical hits always use a **1.0× flat multiplier** regardless of hitbox.

### Hitbox Weight Table

Hits are distributed via weighted random selection across 14 regions:

| Hitbox | Weight | Multiplier | Vital? |
|---|---|---|---|
| Head | 5 | 3.5× | Yes |
| Throat | 3 | 3.5× | Yes |
| Heart | 2 | 3.5× | Yes |
| Chest | 15 | 2.0× | No |
| Stomach | 10 | 2.0× | No |
| Left Arm | 8 | 1.0× | No |
| Right Arm | 8 | 1.0× | No |
| Left Leg | 10 | 1.0× | No |
| Right Leg | 10 | 1.0× | No |
| Left Hand | 4 | 0.7× | No |
| Right Hand | 4 | 0.7× | No |
| Left Foot | 3 | 0.7× | No |
| Right Foot | 3 | 0.7× | No |
| Groin | 5 | 2.0× | No |

Total weight: **90**. The probability of any hitbox = `weight / 90`.

### Final Damage Calculation

```
rawDamage  = baseUnarmedDamage  (50 by default, configurable)
multiplier = isCrit ? hitbox.damageMultiplier : 1.0
finalDmg   = floor( rawDamage × multiplier × (1 − mitigation) )
```

### Dynamic Shop Pricing

```
currentPrice = basePrice × (referenceSupply / currentSupply) ^ elasticity
```

| Parameter | Default value |
|---|---|
| `elasticity` | 0.5 |
| `minPriceRatio` | 0.20 (20% of base) |
| `maxPriceRatio` | 5.00 (500% of base) |
| `referenceSupply` | 100 units |
| Sales tax | 5% on proceeds |

Buying an item decreases supply → price rises.
Selling an item increases supply → price falls.
Price is always clamped to `[base × min, base × max]`.

### Crime Success Rate

```
ceBonus      = min(0.20, crimeExperience / 5000.0)
successRate  = min(0.95, baseCrimeSuccessRate + ceBonus)
```

A player with 5,000+ CE gets the full +20% bonus on every crime.
Hard cap at 95% — no crime is ever guaranteed.

---

## Data Files

All balancing lives in plain JSON. No recompile needed to tweak values.

| File | Purpose |
|---|---|
| `data/game_config.json` | Regen intervals, caps, combat constants, shop elasticity |
| `data/items.json` | 20 item definitions (Weapon / Armor / Consumable) |
| `data/world.json` | 8 city locations with adjacency connections |
| `data/npcs.json` | 4 NPC templates with stats, patrol timing, and FSM config |
| `data/missions.json` | 5 ordered tutorial missions for George |
| `data/jobs.json` | 4 career paths with rank requirements and daily gains |
| `data/crimes.json` | 10 crime definitions with nerve costs and reward ranges |
| `data/destinations.json` | 11 travel destinations with exact flight times |
| `data/savegame.json` | *(generated — gitignored)* Player save; created automatically on first run |
| `data/shop_state.json` | *(generated — gitignored)* Live shop supply levels; created automatically on first run |

---

## Getting Started

### Prerequisites
- Java 17 or later
- JARs already included in `lib/`:
  - `lanterna-3.1.2.jar`
  - `jackson-databind-2.16.1.jar`
  - `jackson-core-2.16.1.jar`
  - `jackson-annotations-2.16.1.jar`

### Compile

**Windows (PowerShell):**
```powershell
cd "path\to\final"
$jars = (Get-ChildItem lib\*.jar | % { $_.FullName }) -join ";"
$srcs = (Get-ChildItem -Recurse src -Filter "*.java" | % { $_.FullName })
javac --release 17 -cp "$jars" -d bin $srcs
```

**Linux / macOS:**
```bash
find src -name "*.java" | xargs javac --release 17 -cp "lib/*" -d bin
```

### Run

```powershell
java -cp "$jars;bin" nottorn.Main      # Windows
```
```bash
java -cp "lib/*:bin" nottorn.Main      # Linux / macOS
```

A Swing terminal window titled **"NotTorn — Text-Based Crime Simulation"** will open.
Progress saves automatically every 60 seconds and on exit to `data/savegame.json`.

---

## Controls

| Key | Action |
|---|---|
| `G` | Talk to George (tutorial missions) |
| `C` | Crime menu |
| `J` | Job enrollment / resign menu |
| `T` | Travel menu — shows outbound destinations, or *"Return to Torn City"* as option 1 when abroad |
| `R` | Shortcut: start return flight home from abroad |
| `B` | Buy from Item Market |
| `S` | Sell items from inventory |
| `I` | Inventory panel |
| `M` | City map panel |
| `L` | Last combat log panel |
| `1`–`9` | Select from open menu |
| `0` / `ESC` | Close menu or panel / quit |

---

## Roadmap / TODO

### Multiplayer
- [ ] Extract game logic into a **server process** with a lightweight TCP/WebSocket API
- [ ] Player-vs-player combat — attack another connected player's character
- [ ] Shared global economy — all players affect the same shop supply levels
- [ ] Faction system — form groups, coordinate crimes, run turf wars
- [ ] Live player list and online status indicators
- [ ] Anti-cheat: all stat mutations validated server-side; client is display-only

### Web Interface
- [ ] Build a **browser client** (HTML + JavaScript) that connects to the game server via WebSocket
- [ ] Replicate the terminal aesthetic in the browser using a canvas or CSS grid renderer
- [ ] REST API endpoints for player stats, inventory, and market data
- [ ] Mobile-friendly layout so the game is playable on phone
- [ ] Persistent accounts with hashed password login
- [ ] OAuth2 sign-in (GitHub / Google)

### Gameplay Expansions
- [ ] Gym training — spend Energy to increase battle stats; Happiness multiplier active
- [ ] Hospital system — players/NPCs who reach 0 HP are hospitalised for a timed duration
- [ ] Jail busting — Law career players can free jailed players via a mini-game
- [ ] Organised crimes — asynchronous multi-player heists with role assignments
- [ ] Faction tech tree — spend Respect to unlock passive bonuses
- [ ] International arbitrage — buy cheap items abroad, sell high at home
- [ ] Stock market — accumulate share blocks for passive item drops
- [ ] Player bazaars — private storefronts with zero tax
- [ ] Merit / education system — spend points to permanently boost stats

### Technical Improvements
- [ ] Replace manual JAR classpath with **Maven** or **Gradle** build
- [ ] Unit test suite (JUnit 5) for combat maths, regen, and economy
- [ ] Docker container for the server component
- [ ] SQLite or PostgreSQL backend to replace JSON save files at scale
- [ ] Hot-reload of data JSON files without restarting the server
- [ ] Proper logging framework (SLF4J + Logback) replacing `System.out`

---

*NotTorn is a fan-inspired academic project. Not affiliated with or endorsed by the creators of Torn City.*

---

**Joel — Student at NSCC** &nbsp;·&nbsp; [gemsns.github.io](https://gemsns.github.io) &nbsp;·&nbsp; [github.com/gemsns](https://github.com/gemsns)
