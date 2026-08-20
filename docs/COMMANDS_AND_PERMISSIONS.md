# 📜 ParrotCouriers — Commands & Permissions Reference

---

## 💻 Commands

All subcommands feature dynamic tab completion and permission checks.

| Command | Permission | Description |
|---|---|---|
| `/courier help` | `parrotcouriers.use` | Displays interactive quickstart instructions and command syntax. |
| `/courier list` | `parrotcouriers.use` | Lists all your active couriers, their current states, and targets. |
| `/courier recall` | `parrotcouriers.recall` | Recalls an in-transit courier or rescues a courier stranded in distant chunks. |
| `/courier claim` | `parrotcouriers.claim` | Opens the Claim GUI for any returned courier waiting for collection. |
| `/courier history` | `parrotcouriers.history` | Opens the transaction ledger showing your last 10 completed trades. |
| `/courier perch set` | `parrotcouriers.perch` | Sets your personal Delivery Perch at the block under your feet. |
| `/courier perch prioritize` | `parrotcouriers.perch` | Toggles automatic landing prioritization at your perch. |
| `/courier perch remove` | `parrotcouriers.perch` | Removes your registered Delivery Perch. |
| `/courier reload` | `parrotcouriers.admin` | Hot-reloads `config.yml` settings without restarting the server. |

---

## 🔑 Permissions Tree

Permissions are structured for standard survival servers and server admins:

```yaml
parrotcouriers.use:
  description: Allows registering and sending courier parrots
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
  description: Grants access to reload configuration and manage all couriers
  default: op
```
