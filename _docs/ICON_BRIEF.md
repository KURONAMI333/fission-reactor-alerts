# Icon design brief — Fission Reactor Alerts

Per LOGO_PLAYBOOK.md §③: addon-mod icons are produced by the project owner,
not generated. This brief only names the host assets and the intended
two-layer composition; no candidate images were produced.

## Classification

③ addon — two-layer syntax: **contribution (constant)** × **host frame (variable)**.

Host mod: **Mekanism Generators** (the fission reactor this addon watches).
What we add: **an early-warning alert** (chat message on first damage).

## Host assets to sample

All inside the already-cached dev jars — no fetch needed:

- `mekanismgenerators-268566-7904061.jar`
  - `assets/mekanismgenerators/textures/block/reactor_controller_on.png` —
    the lit controller face; reads as "reactor" even at 48 px (animated glow)
  - `assets/mekanismgenerators/textures/block/fission_reactor_casing.png` —
    neutral frame/casing tile
  - `assets/mekanismgenerators/textures/block/reactor_frame.png` —
    alternative frame with the dark port silhouette
- `mekanism-268560-7904058.jar`
  - `assets/mekanism/textures/block/models/industrial_alarm.png` —
    Mekanism's own Industrial Alarm texture. Players already associate this
    device with reactor warnings, so it is the natural contribution symbol —
    keeps the icon inside the host's visual language instead of inventing one.

## Suggested composition

- **Contribution layer (main)**: the Industrial Alarm (or a red warning
  triangle if the alarm reads too small at 64 px).
- **Host layer (frame)**: reactor controller / casing tile as background or
  corner framing, per the two-layer pattern — host elements stay clearly
  Mekanism-coloured so the icon reads "Mekanism addon" on the store shelf.

## Readability check

The store grid renders ~64 px; verify the alarm silhouette and the reactor
frame are still distinguishable at that size before committing.

## Output spec

- `src/main/resources/logo.png` in-jar + `branding/fissionreactoralerts_icon_512/256/128/64.png` exports
  (per LOGO_PLAYBOOK output convention).
