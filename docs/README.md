# 🦜 ParrotCouriers — Complete Plugin Documentation

**Version:** 1.0.0  
**Target Platform:** PaperMC 26.2 (Java 25)  
**Author:** Dxrmy  

---

## 📖 Overview

**ParrotCouriers** transforms vanilla tamed parrots into intelligent, autonomous postal couriers. Players can send secure item deliveries, trading packages, and written letters across the Overworld, Nether, and The End.

Couriers navigate complex 3D environments (caves, ravines, Nether fortress corridors, mountain ranges) using a **Baritone-grade 3D A\* flight engine** with real-time sensory obstacle repulsion, persistent chunk loading, and emergency recovery mechanisms.

---

## 📦 Contents of this Directory

| File | Description |
|---|---|
| [`ParrotCouriers-v1.0.0.jar`](file:///C:/Users/kmric/Documents/Minecraft%20Mods%20and%20Plugins/Plugins/ParrotCouriers/ParrotCouriers-v1.0.0.jar) | Production-ready plugin JAR file ready to drop into your server's `plugins/` folder |
| [`MANUAL.md`](file:///C:/Users/kmric/Documents/Minecraft%20Mods%20and%20Plugins/Plugins/ParrotCouriers/MANUAL.md) | Complete in-game player guide, step-by-step setup, buffs, and mailbox perches |
| [`COMMANDS_AND_PERMISSIONS.md`](file:///C:/Users/kmric/Documents/Minecraft%20Mods%20and%20Plugins/Plugins/ParrotCouriers/COMMANDS_AND_PERMISSIONS.md) | Command syntax, subcommands, tab completion, and permissions tree |
| [`CONFIGURATION.md`](file:///C:/Users/kmric/Documents/Minecraft%20Mods%20and%20Plugins/Plugins/ParrotCouriers/CONFIGURATION.md) | Full `config.yml` guide with default values and tuning advice |
| [`ARCHITECTURE_AND_SPECS.md`](file:///C:/Users/kmric/Documents/Minecraft%20Mods%20and%20Plugins/Plugins/ParrotCouriers/ARCHITECTURE_AND_SPECS.md) | In-depth technical architecture (3D A\*, Sensory Repulsion, Chunk Tickets, Anti-Dupe Engine) |

---

## 🚀 Quick Start Guide

### 1. Register a Courier
1. Tame a parrot using seeds.
2. Name it with a Name Tag using `@` prefix:
   - To send to a player: `@Dormy`
   - To send to coordinates: `@100 64 -200` (or `@nether -50 70 120`)

### 2. (Optional) Apply Special Courier Buffs
- **Sweet Berries:** Sneak right-click to grant **+85% Flight Speed** and wind trails.
- **Glow Berries:** Sneak right-click to grant **Night Glowing Outline**.
- **Chorus Fruit:** Sneak right-click to unlock **Interdimensional Travel** (Nether / End).
- **Written Book / Paper:** Right-click with a book or named note to attach a readable letter.

### 3. Load Items & Launch
- **Step 1:** Right-click with the item/package you want to send.
- **Step 2:** Right-click with the payment item you require from the recipient (or sneak-click with an empty hand for a free gift).
- **Step 3:** The courier locks into flight and begins navigating toward the target!

### 4. Recipient Acceptance & Return Flight
- When the courier arrives, the recipient sneaks near it to open the trade GUI.
- The recipient collects the package and deposits any required payment (or optional tip).
- The courier flies back to the owner or their registered **Delivery Perch** where the sender sneaks to collect their items.
