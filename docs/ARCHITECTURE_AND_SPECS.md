# ParrotCouriers Technical Architecture

## 1. 3D Flight Navigation & Collision Handling

- **3D A* Pathfinding (`Pathfinder3D.java`):**
  - Searches up to 10,000 nodes with a 0.6x0.9x0.6m entity bounding box clearance to avoid 1-block gaps.
  - Hierarchical sub-goal chaining breaks paths longer than 35 blocks into 30m intermediate waypoints.
  - Waypoints are cached per courier UUID (`CachedPath`) to avoid per-tick recalculations.

- **Sensory Repulsion Potential Field (`FlightEngine.java`):**
  - Evaluates 14 probe rays in a hemisphere around the flight velocity vector up to 4 meters away.
  - Applies an inverse-square repulsive force away from nearby solid blocks to navigate around walls and columns.

- **Collision Slide Guard:**
  - Performs 1-tick lookahead checks. If an obstacle is ahead, velocity is projected onto the surface tangent plane to prevent block clipping.

- **Stuck Detection & Teleport Fallback:**
  - If position changes by less than 0.45m over 50 ticks (2.5s), an escape impulse is applied towards the nearest open air space.
  - If 3 escape attempts fail or the flight time exceeds the calculated ETA timeout, the courier performs an emergency warp to the target location.

---

## 2. Chunk Ticket & Entity Lifecycle Management

- **Dynamic Chunk Tickets:**
  - While in transit, couriers hold a plugin chunk ticket on their current chunk to keep their flight path ticking.
  - Chunk tickets are immediately released upon arrival or completion to prevent unused chunks from remaining in memory.

- **Entity Persistence:**
  - Couriers are marked `persistent = true`, `invulnerable = true`, and `removeWhenFarAway = false`.
  - All package states, payment requirements, and original parrot metadata (including variant color and custom name) are stored in both the entity's PersistentDataContainer (PDC) and serialized to `couriers.yml`.

---

## 3. Trade Interface & Duplication Prevention (`TradeGui.java`)

- **Payload Clearing:** The payload stack is cleared from the courier data immediately when a trade is accepted (`courierData.setPayloadItem(null)`), preventing items from being collected twice upon return.
- **Inventory Overflow:** If a player's inventory is full when accepting a delivery or claiming returns, remaining items are dropped naturally at their feet with a chat notification.
