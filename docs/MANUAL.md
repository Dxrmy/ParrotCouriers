# ParrotCouriers Player Guide

## Setup and Workflow

1. **Taming & Registration:**
   - Tame a parrot using regular seeds.
   - Apply a Name Tag with `@` prefix:
     - `@PlayerName` to send directly to a player.
     - `@X Y Z` or `@world X Y Z` to send to specific coordinates.

2. **Applying Buffs (Optional):**
   - **Sweet Berries:** Sneak right-click to grant +85% speed with trail particles.
   - **Glow Berries:** Sneak right-click to add a persistent glow effect for night tracking.
   - **Chorus Fruit:** Sneak right-click to allow the courier to warp across dimensions.
   - **Book & Quill / Written Book:** Right-click to attach a letter that the recipient can read.

3. **Loading Items:**
   - Right-click with the item stack you wish to send.
   - Right-click with the payment item you want in exchange (or sneak right-click with an empty hand if no payment is required).
   - The parrot will immediately take flight and begin pathfinding to the destination.

4. **Receiving & Trading:**
   - Sneak near the courier when it arrives to open the trade interface.
   - Click the letter slot to open and read the attached book.
   - Deposit the required payment items (or an optional tip) into the right slot and click Accept.
   - The courier flies back to the sender's location or their Delivery Perch.

5. **Claiming Returned Items:**
   - Sneak near the returned courier or use `/courier claim` to open the collection interface.

---

## Delivery Perches

A Delivery Perch serves as a permanent landing mailbox for incoming and returning couriers.

- `/courier perch set` — Registers the block under your feet as your active perch.
- `/courier perch prioritize` — Toggles whether couriers land on your perch or search for you directly.
- `/courier perch remove` — Unregisters your current perch.

---

## Recalling Couriers

If you need to cancel a delivery:
- Run `/courier recall`.
- If the courier is flying, it will turn around and fly back to you.
- If the courier was located in an unloaded chunk, it will safely return directly to your side with its items intact.
