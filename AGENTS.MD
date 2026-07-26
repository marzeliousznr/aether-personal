# AGENTS.MD — Guide for AI Agents

This file provides guidance to AI coding agents working with this repository.

## Overview

Aether is a Fabric client-side mod for Hypixel Skyblock (farming QoL: auto farming,
pest handling, failsafes, visual spoofers). It targets **Minecraft 26.1.2** and
**Java 25**. There is no test suite; verification is done by building and running in-game.

## Commands

```bash
./gradlew build            # compile + produce the mod jar in build/libs/
./gradlew clean build
./gradlew runClient        # launch a dev Minecraft client with the mod (fabric-loom)
```

- The artifact version comes from `aether_version` in `gradle.properties`. CI rebuilds
  on push to `main`, auto-computes a `X.Y.Z-rN` release tag, publishes a GitHub release,
  and posts a Discord webhook. Do not hand-edit release tags.
- Optional translation packs are fetched at build time only when `-PaetherTranslationRepoRawUrl`
  and `-PaetherTranslationLocales` (or the matching env vars) are set; otherwise the fetch is skipped.
- Always kill gradlew processes when done compiling.

## Architecture

Everything runs on the client. The entrypoint declared in `src/main/resources/fabric.mod.json`
is `dev.aether.AetherClient` (`ClientModInitializer`). `dev.aether.Aether` is a no-op
`ModInitializer` and is not wired in. Startup order in `AetherClient.onInitializeClient`:
config → proxy → install bootstrap hooks → `ClientFeatureBootstrap.initialize()`.

### Mixin ↔ Feature Decoupling (Most Important Pattern)

Mixins in `dev.aether.mixin` must not depend on feature code directly. Instead they call
**static methods on `dev.aether.bootstrap.AetherBootstrapHooks`**, which delegate to an
installed `FeatureHooks` implementation. The live implementation is
`dev.aether.feature.LiveAetherBootstrapHooks`, installed at startup. `FeatureHooks` is an
interface of `default` no-op methods, so `AetherBootstrapHooks` returns safe defaults before
install and after `reset()`.

To wire a new mixin behavior into feature code:
1. Add a method to the `FeatureHooks` interface
2. Add the delegating static in `AetherBootstrapHooks`
3. Implement it in `LiveAetherBootstrapHooks`
4. Call the static from the mixin

Mixins are registered in `src/main/resources/aether.mixins.json` (`compatibilityLevel: JAVA_25`);
adding a mixin class requires adding it there.

### Feature Wiring

`dev.aether.feature.ClientFeatureBootstrap.initialize()` is the single place that boots all
runtime subsystems (config load, sound/theme/profit managers, macro worker thread, HUD registry,
world render events, and the `AetherScreenHooks` / `AetherChatEvents` / `AetherCommandRegistrar`
/ `AetherTickHandlers` registrations in `dev.aether.bootstrap`). `shutdown()` tears them down on
client stop; `onConfigProfileLoaded` re-syncs after a config profile switch. Register new
managers/tick handlers here.

### Modules

`dev.aether.modules.*` holds feature logic, generally as singleton `*Manager` classes with static
`syncFromConfig` / tick / start-stop methods called from bootstrap and tick handlers.

Key module groups:
- `failsafe/` — Detection and response system (BPS, ghost block, dirt, rotation, inventory)
- `pathfinding/` — A* pathfinding, etherwarp, fly navigation (~50 files)
- `pest/` — Pest detection, destruction, exchange, traps, sprayonator
- `visitor/` — Auto visitor handling
- `farming/` — Mouse ungrab, squeaky mousemat, fast lane switch
- `profit/` — Profit tracking (session/lifetime/daily) with bazaar pricing
- `session/` — Dynamic rest, restart, recovery, daily farm time
- `visuals/` — Freecam, freelook, streamer mode,PIP
- `discord/` — Discord bot remote control (opt-in, user-provided bot token)
- `rotation/` — Server-side rotation spoofing
- `gear/` — Gear/loadout management, budget autopet
- `inventorymanager/` — Auto sell, book combine, junk management
- `performance/` — Performance mode, mute
- `movement/` — Movement recording/playback

### Macros

`dev.aether.macro`: `AbstractMacro` is a per-tick state machine (`updateState` decides state,
`invokeState` presses keys via `holdKeys`; keys not listed are released). Concrete farms live in
`macro/impl`. **All background macro work runs through the single `MacroWorkerThread` singleton**
(`submit(label, runnable)`), never `new Thread(...)`. Runnables may block, but any call touching
Minecraft game state must be dispatched via `client.execute(...)`.

Macro implementations: SShapeCropMacro, SShapeSugarCaneMacro, SDSMushroomMacro, WSCropMacro,
WSFarmMacro, ADFarmMacro, CocoaBeansMacro, CustomFarmMacro.

### Config

`dev.aether.config.AetherConfig` is the central registry: every setting is a
`public static final ConfigEntry<T>` created with a `Config.bool/integer/floatVal/doubleVal/...`
factory, which both builds the entry and registers it for JSON persistence to `aether_config.json`.
Read with `.get()`, write with `.set(...)`; call `AetherConfig.save()` after mutating from UI code.
Entry types are in `config/entries`. Profiles, presets, and themes have dedicated `*Manager`/
`*PresetManager` classes.

### UI

`dev.aether.ui.MainGUI` is a custom NanoVG-rendered screen (LWJGL nanovg via
`dev.aether.renderer.NVGRenderer` / `NanoVGManager`), not vanilla widgets. Settings pages are
contributed through a **`ServiceLoader` registry**: implementations of
`dev.aether.ui.MainGUIRegistryProvider` are listed in
`src/main/resources/META-INF/services/dev.aether.ui.MainGUIRegistryProvider`, and each registers a
tab/subtab of `Setting` objects (`ui/settings`: `ToggleSetting`, `SliderSetting`, `DropdownSetting`,
`ActionSetting`, `TextSetting`, ...). **To add a settings page you must add your provider class to
that services file** — it is not auto-discovered otherwise. `/aether` opens the GUI.

### Other Subsystems

- `dev.aether.proxy` — SOCKS proxy support (netty), init'd early in `AetherClient`.
- `dev.aether.hud` — Draggable HUD elements registered via `HudRegistry`.
- `dev.aether.notification` — In-game + desktop (JNA) notifications.
- `dev.aether.renderer` — NanoVG/SVG rendering, blur framebuffer, world-space highlighters.
- `dev.aether.update` — Read-only update checker (notify-only, never installs).
- Failsafe sounds use the bundled mp3 libs; all native deps are shaded into the jar via Gradle `include(...)`.

## Core Rules

### Commits
- Commit frequently.
- Format: `type: description` (e.g. `feat: add user auth`, `fix: null check on login`)
- Do not add yourself as co-author
- When pushing, always update `CHANGELOG.md` with a entry under `[Unreleased]`

### Code
- Do not use comments that are over two lines long.
- Do not add unnecessary comments to every single thing.
- Kill gradlew processes when done compiling.

### Security
- This app never auto-updates. The auto-update mechanism has been removed.
- Update checking is notify-only via `UpdateChecker.checkAndNotify()`.
- All outbound network endpoints are documented in `UPDATES.MD`.
- Do not add new outbound network connections without documenting them.
- Do not add ProcessBuilder calls that touch the app's jar/install directory.
- Do not add code that downloads and writes files without explicit user consent.

## Project Structure

```
src/main/java/dev/aether/
├── Aether.java              # No-op ModInitializer
├── AetherClient.java        # Primary entry point (ClientModInitializer)
├── bootstrap/               # Mixin bridge, tick handlers, commands, screen hooks
├── config/                  # Config system, profiles, presets, enums
│   └── entries/             # BooleanEntry, IntEntry, FloatEntry, etc.
├── feature/                 # Feature bootstrap and live hook implementation
├── hud/                     # HUD elements, registry, editor screen
├── macro/                   # Macro state machines, worker thread
│   └── impl/                # Concrete farm macros
├── mixin/                   # 56 Minecraft class accessors and injectors
├── modules/                 # All feature logic
│   ├── discord/             # Discord bot remote control
│   ├── failsafe/            # Failsafe detection and response
│   ├── farming/             # Farming utilities
│   ├── forge/               # Forge management
│   ├── gear/                # Gear/loadout management
│   ├── inventorymanager/    # Inventory automation
│   ├── metaldetector/       # Metal detector solver
│   ├── misc/                # Auto carnival
│   ├── movement/            # Movement recording/playback
│   ├── pathfinding/         # A* pathfinding, etherwarp, fly
│   ├── performance/         # Performance mode, mute
│   ├── pest/                # Pest handling
│   ├── profit/              # Profit tracking
│   ├── rotation/            # Rotation spoofing
│   ├── session/             # Session management
│   ├── visitor/             # Visitor handling
│   └── visuals/             # Freecam, freelook, streamer mode
├── notification/            # Notification system
├── proxy/                   # SOCKS proxy support
├── renderer/                # NanoVG/SVG rendering, blur, world effects
├── update/                  # Update checker (notify-only)
└── util/                    # Shared utilities

src/main/resources/
├── fabric.mod.json          # Mod descriptor
├── aether.mixins.json       # Mixin config (56 mixins)
├── META-INF/services/       # ServiceLoader registry for UI providers
├── pack_fallback.zip        # Fallback resource pack
└── assets/aether/
    ├── shaders/             # GLSL shaders (ripple, failsafe flash)
    ├── sounds/              # Failsafe sounds
    ├── lang/                # Translations
    ├── fonts/               # Inter font family
    ├── icons/               # SVG icons
    └── humanization-presets/ # Movement presets
```

## Network Endpoints

| Endpoint | Purpose | Required |
|----------|---------|----------|
| `api.github.com/repos/marzeliousznr/aether-personal` | Update check (read-only) | Yes |
| `resourcepacks.hypixel.net` | SkyBlock resource pack fallback | Yes |
| `sky.coflnet.com` | Bazaar pricing data | Optional (profit tracking) |
| `api.elitebot.dev` | Feast timer data | Optional (dynamic pests) |
| `discord.com/api/v10` | Discord bot remote control | Optional (user-configured) |
