# ParrotCouriers Commands & Permissions

## Commands

| Command | Permission | Description |
|---|---|---|
| `/courier help` | `parrotcouriers.use` | Displays interactive quickstart help and command reference. |
| `/courier list` | `parrotcouriers.use` | Lists all active couriers registered by you. |
| `/courier recall` | `parrotcouriers.recall` | Recalls an en-route courier or rescues a stranded courier. |
| `/courier claim` | `parrotcouriers.claim` | Opens the claim interface for any returned courier waiting for collection. |
| `/courier history` | `parrotcouriers.history` | Displays your recent completed deliveries and payments. |
| `/courier perch set` | `parrotcouriers.perch` | Sets your Delivery Perch at the block under your feet. |
| `/courier perch prioritize` | `parrotcouriers.perch` | Toggles whether couriers prioritize landing at your perch. |
| `/courier perch remove` | `parrotcouriers.perch` | Removes your registered Delivery Perch. |
| `/courier reload` | `parrotcouriers.admin` | Reloads `config.yml`. |

---

## Permissions

```yaml
parrotcouriers.use:
  description: Allows player to use parrot couriers
  default: true

parrotcouriers.perch:
  description: Allows setting and prioritizing personal delivery perches
  default: true

parrotcouriers.history:
  description: Allows viewing personal trade history
  default: true

parrotcouriers.recall:
  description: Allows recalling active couriers
  default: true

parrotcouriers.claim:
  description: Allows claiming returned courier payments via /courier claim
  default: true

parrotcouriers.admin:
  description: Grants access to reload configuration and admin tools
  default: op
```
