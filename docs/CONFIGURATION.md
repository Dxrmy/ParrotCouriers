# ⚙️ ParrotCouriers — Configuration Reference (`config.yml`)

---

## 📄 Complete Annotated `config.yml`

```yaml
# ================================================================= #
#                     ParrotCouriers Configuration                  #
#                 Compatible with Minecraft / Paper 26.2            #
# ================================================================= #

# Targeting & Registration
targeting:
  # Prefix required on Name Tag to register a courier (e.g. "@Dormy" or "@100 64 -200")
  prefix: "@"
  # Require the prefix for courier registration (prevents regular pet names like 'bob' from triggering courier mode)
  require-prefix: true
  # Force parrot to sit and stay still while owner is loading items
  freeze-during-setup: true

# Flight & Navigation
flight:
  # Base flight speed in blocks per tick (0.28 is ~5.6 blocks/sec - calm, relaxed & steady)
  speed: 0.28
  # Speed boost multiplier when fed Sweet Berries during setup (1.85 = +85% speed boost with wind trails!)
  speed-boost-multiplier: 1.85
  # Minimum cruising height above terrain blocks (avoids mountains/trees)
  terrain-clearance: 6.0
  # Cruising altitude base level in open skies
  cruise-altitude: 85.0
  # Deceleration factor when approaching landing (smoother glide)
  approach-speed-factor: 0.55

  # Stuck Detection & Re-pathing
  # Number of ticks without movement before applying an escape impulse and recalculating 3D A* path (20 ticks = 1s)
  stuck-retry-ticks: 50
  # Maximum escape attempts before triggering Emergency Teleport Rescue directly to the target
  max-stuck-retries: 3

  # ETA & Timeout Safety
  # Base timeout in seconds before emergency teleport rescue triggers
  timeout-base-seconds: 40
  # Additional timeout seconds added per 100 blocks distance to destination
  timeout-per-100-blocks-seconds: 12

  # Particles (Options: WAX_OFF, CHERRY_LEAVES, HAPPY_VILLAGER, END_ROD, FEATHER, ENCHANT)
  trail-particle: "WAX_OFF"
  trail-particle-count: 1
  hover-particle: "HAPPY_VILLAGER"
  hover-particle-count: 1
  lock-particle: "HEART"
  lock-particle-count: 10

# Perches & Landing Preferences
perches:
  # If true, couriers automatically prioritize landing on a player's registered Perch on both delivery and return
  prioritize-perches: true

# Inter-Dimensional Flight (Overworld <-> Nether <-> The End)
interdimensional:
  # If true, couriers can warp across dimensions
  enabled: true
  # If true, couriers MUST be fed a Chorus Fruit during setup to unlock cross-dimensional travel
  require-chorus-fruit: true

# Interaction & Delivery
delivery:
  # Interaction sneak detection radius in blocks
  interaction-radius: 3.5
  # Action bar notification reminder interval (20 ticks = 1 second)
  actionbar-interval-ticks: 35
  # Live ETA action bar updates for sender and recipient while courier is in flight
  show-eta-actionbar: true
  # Allow free deliveries (empty hand sneak right-click)
  allow-free-deliveries: true

# Visual Cues & Animations
visual-cues:
  # If true, parrots perform head-bobbing / note animations while waiting
  dancing-while-waiting: false
  # Enable subtle ambient particles around couriers
  state-particles: true
  # Show floating nametags above couriers (set to false for minimal/immersive visual cues)
  show-nametags: true

# Sounds
sounds:
  enabled: true
  chime-sound: "BLOCK_NOTE_BLOCK_CHIME"
  bell-sound: "BLOCK_NOTE_BLOCK_BELL"
  complete-sound: "ENTITY_PLAYER_LEVELUP"
  ambient-sound: "ENTITY_PARROT_AMBIENT"
  lock-sound: "ENTITY_PARROT_EAT"

# Clean & Customizable Messages (MiniMessage format, solid colors)
messages:
  show-prefix: true
  prefix: "<gold>[ParrotCouriers]</gold> "
  registered-title: "<green><b>Parrot Courier Registered!</b></green>"
  registered-destination: "<gray>Destination (<yellow>%type%</yellow>): <gold>%target%</gold></gray>"
  step1-prompt: "<yellow><b>Step 1:</b> Right-click with your package to send (or sneak-click with berries/fruit/book for buffs).</yellow>"
  step2-prompt: "<yellow><b>Step 2:</b> Right-click with your payment (or sneak-click empty hand for free gift).</yellow>"
  payload-loaded: "<green>✔ Package loaded: <gold>%item% x%amount%</gold>!</green>"
  payment-set: "<green>✔ Required payment: <gold>%item% x%amount%</gold>.</green>"
  gift-delivery-set: "<green>✔ Free gift delivery (no payment required)!</green>"
  trade-locked: "<green><b>Trade locked!</b> Courier is taking flight to <gold>%target%</gold>...</green>"
  trade-locked-sub: "<gray>Courier is invulnerable and flying to: <yellow>%target%</yellow>.</gray>"
  actionbar-delivery: "<gold><b>[Courier]</b> Delivery from <yellow>%owner%</yellow> - <green>Sneak to accept</green></gold>"
  actionbar-returned: "<gold><b>[Courier]</b> Your courier returned! - <green>Sneak to collect</green></gold>"
  trade-completed-recipient: "<green>✔ Trade accepted! Package received.</green>"
  trade-completed-owner: "<green>✔ Your courier delivered the package to <yellow>%recipient%</yellow> and is returning!</green>"
  payment-claimed: "<green>✔ Items claimed! Your parrot is resting safely.</green>"
  cancelled: "<yellow>Courier recalled! The parrot is flying back to you.</yellow>"
```
