# 🏗️ ParrotCouriers — Technical Architecture & Specifications

---

## 1. Baritone-Grade 3D Flight & Sensory Physics

The courier flight navigation system in [`FlightEngine.java`](file:///C:/Users/kmric/ParrotCouriers/src/main/java/com/parrotcouriers/flight/FlightEngine.java) and [`Pathfinder3D.java`](file:///C:/Users/kmric/ParrotCouriers/src/main/java/com/parrotcouriers/flight/Pathfinder3D.java) is engineered for full 3D terrain navigation:

### A. Hierarchical 3D A\* Planner (10,000 Node Depth)
- **Hitbox Clearance:** Enforces a $0.6 \times 0.9 \times 0.6\text{m}$ collision bounding box to prevent clipping into 1-block crevices or low ceilings.
- **Hierarchical Sub-Goals:** For distant flights ($>35\text{m}$), journeys are automatically segmented into 30m local air-pocket waypoints, chaining A\* searches with zero tick overhead.
- **Path Caching:** Waypoint arrays are cached per courier UUID (`CachedPath`) to eliminate per-tick recalculations.

### B. 14-Ray Sensory Repulsion Potential Field
The entity projects a 3D hemispherical array of 14 probe rays around its velocity vector extending up to 4.0 meters. Any solid block detected exerts an inverse-square repulsion force:
$$\vec{F}_{\text{repel}} = \sum_{i=1}^{14} \frac{-\hat{r}_i \cdot (4.0 - d_i)}{d_i^2 \times 1.5}$$
Blending $\vec{F}_{\text{repel}}$ with the target attraction vector creates organic, wall-bending flight curves around Nether fortress pillars, stalactites, and cavern walls.

### C. Predictive Collision Sliding Guard
Predicts block intersections 1 tick ahead. If an obstacle is detected in front, velocity is projected onto the surface tangent sliding plane, allowing the parrot to skim along walls without sticking or penetrating solid blocks.

### D. Stuck Detection & Escape Impulses
- Monitored at $\Delta \text{pos} < 0.45\text{m}$ for 50 ticks ($2.5\text{s}$).
- Triggers an instant escape impulse vector into adjacent open airspace with cloud puff particles.
- If 3 retries fail or total flight time exceeds maximum ETA, the courier initiates **Emergency Teleport Rescue** straight to the target/perch.

---

## 2. Active Chunk-Ticket Management & Persistence

- **In-Flight Chunk Tickets:** As the courier moves across chunk boundaries, `world.addPluginChunkTicket(chunkX, chunkZ, plugin)` is invoked, ensuring chunks along the flight path remain loaded and ticking until arrival.
- **Unloaded Chunk Wakeup:** Recalling or finding couriers across unvisited territory dynamically awakens distant chunks (`world.getChunkAt(chunkX, chunkZ)`).
- **Zero-Loss Data Persistence:**
  - Couriers are marked `setPersistent(true)`, `setInvulnerable(true)`, and `setRemoveWhenFarAway(false)`.
  - All inventory stacks (payloads, payments, letters) are serialized into Base64 NBT via `ItemStack.serializeAsBytes()` and written to the entity's PersistentDataContainer (PDC) and `couriers.yml`.
  - Parrot feather color/variant (`Parrot.Variant`) is preserved across all restarts and recoveries.

---

## 3. Anti-Dupe Transaction Engine ([`TradeGui.java`](file:///C:/Users/kmric/ParrotCouriers/src/main/java/com/parrotcouriers/gui/TradeGui.java))

- **Immediate Payload Nullification:** Upon trade confirmation, `courierData.setPayloadItem(null)` is invoked instantaneously before initiating the return flight, permanently preventing item duplication.
- **Symmetrical 1-Slot Padding:** Clean dark-border GUI layout with package slot (10) and payment/tip slot (16).
- **Full Inventory Overflow Drop:** Any overflow items that exceed player inventory space are dropped naturally at the recipient's or owner's feet (`world.dropItemNaturally`) with chat feedback.
