# ParrotCouriers

A lightweight, feature-complete PaperMC plugin that transforms tamed parrots into autonomous, postal couriers capable of delivering items, trade packages, and written letters across dimensions.

Couriers calculate 3D flight paths around terrain, caves, and structures, keep chunks loaded during long-distance transit, and offer a secure trading interface for package exchanges.

## Features

- **3D Flight Pathfinding**: Couriers navigate around terrain, trees, caverns, and Nether structures using 3D path planning and sensory obstacle avoidance.
- **Dynamic Chunk Loading**: Automatically keeps chunks loaded along the courier's flight path, preventing couriers from freezing in unvisited or unloaded chunks.
- **Courier Buffs**:
  - **Sweet Berries**: Grants +85% flight speed boost with cloud trails.
  - **Glow Berries**: Adds a glowing outline for nighttime visibility.
  - **Chorus Fruit**: Unlocks cross-dimensional travel (Overworld, Nether, and The End).
  - **Book & Quill / Written Books**: Attach readable letters that recipients can open directly on screen.
- **Delivery Perch System**: Register personal landing blocks (`/courier perch set`) so couriers land at a designated mailbox rather than chasing a moving player.
- **Live Action Bar Tracking**: Displays real-time distance and ETA countdowns for both sender and recipient while a courier is in flight.
- **Secure Trade Interface**: Symmetrical container GUI where recipients collect packages and submit required payments or optional tips.
- **Anti-Dupe & Inventory Safety**: Instant payload clearing upon trade completion and natural ground drops if player inventory is full.
- **Delivery History**: Track completed transactions and payments with `/courier history`.
- **Emergency Recovery**: Stuck detection with escape impulses, arrival timeouts, and `/courier recall` to safely retrieve couriers from anywhere.

## Requirements

- **Server**: Paper 26.2+ (or Purpur / compatible forks)
- **Java**: Java 25+
- **Dependencies**: None (100% standalone, no external plugins required)

## Installation

1. Download the latest `ParrotCouriers.jar` from [Releases](https://github.com/Dxrmy/ParrotCouriers/releases).
2. Place `ParrotCouriers.jar` into your server's `plugins/` directory.
3. Restart your server to generate the default configuration.

## Usage

### 1. Registering a Courier
1. Tame a parrot with seeds.
2. Name it with a Name Tag using the `@` prefix:
   - Target a player: `@PlayerName`
   - Target coordinates: `@100 64 -200` (or `@nether -50 70 120`)

### 2. Applying Buffs (Optional)
- Sneak + right-click with **Sweet Berries** for speed boost.
- Sneak + right-click with **Glow Berries** for glowing outline.
- Sneak + right-click with **Chorus Fruit** for interdimensional travel.
- Right-click with a **Book & Quill** or **Written Book** to attach a letter.

### 3. Loading Items & Launching
- **Step 1:** Right-click the parrot with the item stack you want to deliver.
- **Step 2:** Right-click with the payment item you require (or sneak right-click with an empty hand for a free delivery).
- The trade locks and the courier takes flight.

### 4. Receiving & Trading
- When the courier arrives, sneak near it to open the trade window.
- Click the letter slot to open and read any attached book.
- Place the required payment into the payment slot (or an optional tip) and click **Accept Delivery**.
- The courier will return to the sender or their registered perch.

### 5. Delivery Perches
Set a designated landing spot so couriers land at your mailbox even when you are offline or moving:
```bash
# Set your delivery perch at your current location
/courier perch set

# Toggle whether couriers prioritize landing at your perch
/courier perch prioritize

# Remove your registered perch
/courier perch remove
```

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/courier help` | `parrotcouriers.use` | Displays interactive command reference. |
| `/courier list` | `parrotcouriers.use` | Lists all your active couriers and destinations. |
| `/courier recall` | `parrotcouriers.recall` | Recalls an en-route courier or recovers a stranded one. |
| `/courier claim` | `parrotcouriers.claim` | Opens the claim interface for returned couriers. |
| `/courier history` | `parrotcouriers.history` | Displays your recent delivery transaction logs. |
| `/courier perch <set\|prioritize\|remove>` | `parrotcouriers.perch` | Manages your personal delivery perch. |
| `/courier reload` | `parrotcouriers.admin` | Reloads `config.yml`. |

```yaml
permissions:
  parrotcouriers.use:
    description: Allows using parrot couriers
    default: true
  parrotcouriers.perch:
    description: Allows setting delivery perches
    default: true
  parrotcouriers.history:
    description: Allows viewing personal trade history
    default: true
  parrotcouriers.recall:
    description: Allows recalling active couriers
    default: true
  parrotcouriers.claim:
    description: Allows claiming returned courier payments
    default: true
  parrotcouriers.admin:
    description: Grants access to reload configuration
    default: op
```

## Configuration

Settings can be customized in `plugins/ParrotCouriers/config.yml`:

```yaml
targeting:
  prefix: "@"
  require-prefix: true
  freeze-during-setup: true

flight:
  speed: 0.28
  speed-boost-multiplier: 1.85
  terrain-clearance: 6.0
  cruise-altitude: 85.0
  approach-speed-factor: 0.55
  stuck-retry-ticks: 50
  max-stuck-retries: 3
  timeout-base-seconds: 40
  timeout-per-100-blocks-seconds: 12

perches:
  prioritize-perches: true

interdimensional:
  enabled: true
  require-chorus-fruit: true

delivery:
  interaction-radius: 3.5
  actionbar-interval-ticks: 35
  show-eta-actionbar: true
  allow-free-deliveries: true

visual-cues:
  dancing-while-waiting: false
  state-particles: true
  show-nametags: true
```

## Building from Source

```bash
git clone https://github.com/Dxrmy/ParrotCouriers.git
cd ParrotCouriers
mvn clean package
```

The compiled plugin `.jar` will be generated in `target/ParrotCouriers.jar`.

## License

MIT License. See [LICENSE](LICENSE) for details.
