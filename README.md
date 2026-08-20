# 🦜 ParrotCouriers

**Version:** 1.0.0  
**Target Platform:** PaperMC 26.2 (Java 25)  
**Author:** Dxrmy  

An advanced PaperMC Minecraft plugin that transforms tamed parrots into autonomous, secure item and letter couriers with Baritone-grade 3D A* flight navigation, obstacle avoidance, customizable mailbox perches, and cross-dimensional travel.

---

## ✨ Features

- **Autonomous 3D A\* Flight Navigation (10,000 Nodes):** Couriers calculate intelligent 3D flight paths across mountains, caves, and Nether terrain.
- **14-Ray Sensory Repulsion Potential Field:** Real-time obstacle detection bends flight smoothly around walls, trees, and fortress pillars.
- **Persistent Chunk-Ticket Flight:** Couriers hold dynamic chunk tickets along their flight corridor to prevent freezing in unloaded chunks.
- **Special Courier Buffs:**
  - 🍇 **Sweet Berries:** Grants +85% speed boost with cloud particle trails.
  - 🌟 **Glow Berries:** Grants nighttime glowing outline.
  - 🔮 **Chorus Fruit:** Unlocks interdimensional flight across Overworld, Nether, and The End.
  - 📜 **Books & Notes:** Attach readable delivery letters and custom notes.
- **Delivery Perch System (Mailboxes):** Register specific blocks as personal perches for automated landing (`/courier perch set` & `/courier perch prioritize`).
- **Live Action Bar ETA:** Real-time distance and ETA countdowns for both sender and recipient.
- **Zero-Dupe & Anti-Theft Protection:** Secure transaction GUI with symmetrical padding, immediate payload consumption, and overflow drop safety.
- **Transaction Ledger:** Track past deliveries and payments with `/courier history`.
- **Emergency Recovery & Recall:** Stuck detection with escape impulses, automatic timeout teleportation rescue, and instant `/courier recall` with 100% feather color/variant preservation.

---

## 🚀 Quick Start Guide

### 1. Register a Courier
1. Tame a parrot using seeds.
2. Name it with a Name Tag using `@` prefix:
   - To send to a player: `@PlayerName`
   - To send to coordinates: `@100 64 -200` (or `@nether -50 70 120`)

### 2. (Optional) Apply Buffs
- **Sweet Berries:** Sneak right-click to grant speed boost.
- **Glow Berries:** Sneak right-click to grant night glow.
- **Chorus Fruit:** Sneak right-click to unlock dimensional travel.
- **Book & Quill / Written Book:** Right-click with a written book or book & quill to attach a letter.

### 3. Load Items & Send
- **Step 1:** Right-click with the package item to send.
- **Step 2:** Right-click with the required payment (or sneak-click empty hand for free gift delivery).
- **Step 3:** The trade locks and the courier takes flight!

### 4. Receive Delivery
- When the courier lands, the recipient sneaks near it to open the trade GUI.
- The recipient collects the package and provides required payment (or optional tip).
- The courier flies back to the owner or their registered **Delivery Perch**.

---

## 💻 Commands

| Command | Permission | Description |
|---|---|---|
| `/courier help` | `parrotcouriers.use` | Displays interactive guide and command syntax. |
| `/courier list` | `parrotcouriers.use` | Lists all your active couriers and their destinations. |
| `/courier recall` | `parrotcouriers.recall` | Recalls an active courier or rescues a stranded one. |
| `/courier claim` | `parrotcouriers.claim` | Opens the Claim GUI for any returned courier. |
| `/courier history` | `parrotcouriers.history` | Views your personal delivery transaction history. |
| `/courier perch set` | `parrotcouriers.perch` | Sets your personal Delivery Perch under your feet. |
| `/courier perch prioritize` | `parrotcouriers.perch` | Toggles prioritizing your perch for landing. |
| `/courier perch remove` | `parrotcouriers.perch` | Removes your registered Delivery Perch. |
| `/courier reload` | `parrotcouriers.admin` | Hot-reloads configuration file. |

---

## 🔑 Permissions

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
  description: Grants access to reload config and administrative tools
  default: op
```

---

## 🛠️ Building & Compilation

Requires **Java 25** and **Maven 3.9+**:

```bash
mvn clean package
```

The compiled plugin JAR will be generated in `target/ParrotCouriers.jar`.
