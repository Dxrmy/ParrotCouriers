# ParrotCouriers v1.0.0 Changelog

### 🦜 Initial Release — ParrotCouriers v1.0.0

Transform tamed parrots into autonomous, secure item and letter couriers across the Overworld, Nether, and The End.

#### ✨ Features
- **3D Flight Pathfinding:** 3D A* flight engine with sensory obstacle avoidance to navigate caves, mountains, and structures.
- **Dynamic Chunk Loading:** Keeps transit corridors loaded so couriers do not freeze in unloaded chunks.
- **Courier Buffs:**
  - 🍇 **Sweet Berries:** +85% speed boost with cloud trails.
  - 🌟 **Glow Berries:** Persistent night-time glowing outline.
  - 🔮 **Chorus Fruit:** Cross-dimensional travel between Overworld, Nether, and The End.
  - 📜 **Book & Quill / Written Books:** Attach readable letters that open directly on screen.
- **Delivery Perch System:** Register personal mailboxes (`/courier perch set`) so couriers land at a designated spot.
- **Live Action Bar ETA:** Real-time distance and arrival countdowns for both sender and recipient.
- **Secure Trade GUI:** Symmetrical container interface for collecting packages, paying required items, or tipping.
- **Anti-Dupe & Inventory Safety:** Instant payload clearing and natural ground drops if recipient inventory is full.
- **Delivery History & Recall:** Track past deliveries with `/courier history` and safely recall couriers with `/courier recall`.

#### 📦 Compatibility
- **Server:** Paper / Purpur `1.21` – `26.2+`
- **Java:** Java 21+
- **Dependencies:** None (100% standalone)
