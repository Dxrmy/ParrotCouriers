# ParrotCouriers

Transform tamed parrots into autonomous, postal couriers capable of delivering items, trade packages, and written letters across the Overworld, Nether, and The End.

Couriers calculate 3D flight paths around terrain, caves, and structures, keep transit chunks loaded during long-distance flights, and provide a secure container trading interface for safe deliveries.

---

## ✨ Features

- **3D Flight Pathfinding**: Couriers navigate around mountains, trees, caverns, and Nether fortress structures using 3D path planning and sensory obstacle avoidance.
- **Dynamic Chunk Loading**: Automatically keeps chunks loaded along the flight path so couriers never freeze or get lost in unloaded territory.
- **Courier Buffs**:
  - 🍇 **Sweet Berries**: +85% speed boost with cloud particle trails.
  - 🌟 **Glow Berries**: Persistent glowing outline for nighttime visibility.
  - 🔮 **Chorus Fruit**: Unlocks cross-dimensional travel across the Overworld, Nether, and The End.
  - 📜 **Book & Quill / Written Books**: Attach readable letters that open directly in a book interface on screen.
- **Delivery Perch System**: Set personal landing blocks (`/courier perch set`) so couriers land at a designated mailbox rather than chasing a moving player.
- **Live Action Bar ETA**: Real-time distance and estimated flight arrival countdowns for both sender and recipient.
- **Secure Trade Interface**: Symmetrical container GUI where recipients collect packages and submit required payments or optional tips.
- **Anti-Dupe & Inventory Safety**: Immediate payload clearing upon trade completion and natural ground drops if recipient inventory is full.
- **Delivery History & Recall**: View past delivery transactions with `/courier history` and safely recall couriers with `/courier recall`.

---

## 🚀 How to Use

### 1. Register a Courier
1. Tame a parrot with seeds.
2. Apply a Name Tag with the `@` prefix:
   - **Send to a player:** `@PlayerName`
   - **Send to coordinates:** `@100 64 -200` (or `@nether -50 70 120`)

### 2. Apply Buffs (Optional)
- Sneak + right-click with **Sweet Berries** for speed boost.
- Sneak + right-click with **Glow Berries** for glowing outline.
- Sneak + right-click with **Chorus Fruit** for interdimensional flight.
- Right-click with a **Book & Quill** or **Written Book** to attach a readable letter.

### 3. Load Items & Launch
- **Step 1:** Right-click the parrot with the item stack you want to send.
- **Step 2:** Right-click with the payment item you require from the recipient (or sneak right-click with an empty hand for a free delivery).
- The trade locks and the courier takes flight.

### 4. Receiving & Trading
- When the courier arrives, sneak near it to open the trade interface.
- Click the letter slot to open and read any attached book.
- Deposit the required payment items (or an optional tip) into the right slot and click **Accept Delivery**.
- The courier flies back to the sender or their registered delivery perch.

---

## 📬 Delivery Perches (Mailboxes)

A Delivery Perch serves as a permanent landing mailbox for incoming and returning couriers:
- `/courier perch set` — Registers the block under your feet as your active perch.
- `/courier perch prioritize` — Toggles whether couriers land on your perch or search for you directly.
- `/courier perch remove` — Unregisters your current perch.

---

## 💻 Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/courier help` | `parrotcouriers.use` | Displays interactive command reference. |
| `/courier list` | `parrotcouriers.use` | Lists all your active couriers and destinations. |
| `/courier recall` | `parrotcouriers.recall` | Recalls an active courier or recovers a stranded one. |
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

---

## 📦 Compatibility & Requirements

- **Server Platform:** Paper / Purpur `1.21` – `26.2+`
- **Java:** Java 21+
- **Dependencies:** **None** (100% standalone, pure Paper API)
- **Clients:** 100% Vanilla compatible (no mods required for players)
