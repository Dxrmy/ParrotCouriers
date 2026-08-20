# ParrotCouriers

**Version:** 1.0.0  
**Target Platform:** PaperMC 26.2 (Java 25)  
**Author:** Dxrmy  

ParrotCouriers is a PaperMC plugin that lets players use tamed parrots to deliver items, trade packages, and written letters across dimensions.

---

## Features

- **3D Flight Navigation:** Couriers use full 3D A* pathfinding and obstacle avoidance to navigate around terrain, trees, caves, and Nether structures.
- **Chunk Loading:** Chunks along the flight path are kept loaded during transit so long-distance deliveries do not stall in unloaded areas.
- **Delivery Buffs:**
  - **Sweet Berries:** Increases flight speed by +85% with cloud particle trails.
  - **Glow Berries:** Adds a glowing outline for night visibility.
  - **Chorus Fruit:** Unlocks cross-dimensional travel between the Overworld, Nether, and The End.
  - **Book & Quill / Written Books:** Attach letters that recipients can read directly from the trade screen.
- **Delivery Perches:** Set personal perches (`/courier perch set`) so couriers land at a designated mailbox block instead of chasing a moving player.
- **Live ETA Action Bar:** Shows real-time distance and estimated flight time for both sender and recipient.
- **Trading GUI:** Symmetrical container interface where recipients can accept deliveries and submit required payments or optional tips.
- **History Ledger:** View past transactions and delivery logs with `/courier history`.
- **Recall & Safety:** Stuck detection with escape impulses, arrival timeouts, and `/courier recall` to safely bring couriers and items home.

---

## Usage Guide

### 1. Register a Courier
1. Tame a parrot with seeds.
2. Apply a Name Tag with the `@` prefix:
   - Target a player: `@PlayerName`
   - Target coordinates: `@100 64 -200` (or `@nether -50 70 120`)

### 2. (Optional) Apply Buffs
- Sneak right-click with **Sweet Berries** for speed boost.
- Sneak right-click with **Glow Berries** for glowing outline.
- Sneak right-click with **Chorus Fruit** for interdimensional travel.
- Right-click with a **Book & Quill** or **Written Book** to attach a letter.

### 3. Load Items & Launch
- **Step 1:** Right-click the parrot with the item you want to send.
- **Step 2:** Right-click with the payment item you require (or sneak right-click with an empty hand for a free delivery).
- The trade locks and the courier takes off.

### 4. Receiving a Delivery
- When the courier arrives, sneak near it to open the trade interface.
- Click the letter slot to read any attached book.
- Deposit the required payment (or an optional tip for free deliveries) and click **Accept Delivery**.
- The courier will fly back to the owner or their registered perch.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/courier help` | `parrotcouriers.use` | Shows plugin help and commands. |
| `/courier list` | `parrotcouriers.use` | Lists all your active couriers and their destinations. |
| `/courier recall` | `parrotcouriers.recall` | Recalls an active courier back to your side. |
| `/courier claim` | `parrotcouriers.claim` | Opens the claim interface for any returned courier. |
| `/courier history` | `parrotcouriers.history` | Shows your recent delivery history. |
| `/courier perch set` | `parrotcouriers.perch` | Sets your delivery perch at your current block. |
| `/courier perch prioritize` | `parrotcouriers.perch` | Toggles whether couriers prioritize landing at your perch. |
| `/courier perch remove` | `parrotcouriers.perch` | Removes your delivery perch. |
| `/courier reload` | `parrotcouriers.admin` | Reloads `config.yml`. |

---

## Permissions

```yaml
parrotcouriers.use:
  description: Allows player to use parrot couriers
  default: true

parrotcouriers.perch:
  description: Allows setting and prioritizing delivery perches
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
  description: Grants access to reload config and admin commands
  default: op
```

---

## Building from Source

Requires **Java 25** and **Maven 3.9+**:

```bash
mvn clean package
```

The compiled JAR will be in `target/ParrotCouriers.jar`.
