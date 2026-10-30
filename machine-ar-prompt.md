# Project brief for Claude Code — "MachineAR": AR part identification for industrial machine maintenance

You are my senior engineer on this project. Read this whole brief first, then **start in plan mode**: ask me the open questions listed at the end, propose the architecture and a phased plan, and wait for my approval before writing code. After approval, build phase by phase, and after each phase run/build/test what you can, summarise what's done, and tell me exactly what I need to test on a physical phone.

---

## 1. The problem

My cousin maintains very large industrial machines. To identify a part and repair it, mechanics currently search through a huge paper/PDF manual and parts catalog. This is slow and error-prone.

## 2. The solution

A mobile AR app with two modes:

- **Mapping mode (Manager):** The manager walks around the machine with a phone. He points the camera at a part, taps on it, and the app saves the part's exact 3D position plus its catalog data (name, part number, specs, manual reference, etc.).
- **Lookup mode (Mechanic):** The mechanic opens the camera and points it at the machine. **All mapped parts in view appear as floating labels/pins** at their saved positions. He taps one and sees the full part data from the catalog.

**No machine learning / object recognition.** Identification is purely spatial: a part is identified by its saved 3D coordinates.

## 3. Core technical concept: marker-based shared coordinate system

Phone AR world coordinates reset every session and differ per device, so we anchor everything to **physical printed markers (stickers)** on the machine.

- Each machine has **one machine coordinate frame**. The first marker the manager registers defines the machine origin.
- Large machines get **multiple markers** (one per side/section). Every marker has a stored pose relative to the machine frame: `T_machine_marker`.
- **All part positions are stored in the machine frame**, so any one visible marker is enough to place every label for the whole machine.

### Math (use 4×4 transforms / pose + quaternion)

- **Localise:** When marker M is detected at `T_world_M`:
  `T_world_machine = T_world_M · inverse(T_machine_M)`
- **Render a part:** `p_world = T_world_machine · p_machine`
- **Map a part:** The manager taps, the app does an AR hit test against detected planes/feature points (fall back to depth if available) and gets `p_world`, then
  `p_machine = inverse(T_world_machine) · p_world`
- **Register a new marker:** While already localised, the manager scans marker B and gets
  `T_machine_B = inverse(T_world_machine) · T_world_B`.
  Average several samples over about 1–2 seconds while the marker is stably tracked, and reject outliers. Show a "hold still…" progress indicator.
- **Drift correction:** Whenever any registered marker is reliably tracked (full tracking state), recompute `T_world_machine` from it. Smooth the update (interpolate over a few frames) so labels don't jump.

### Markers

- Use AR image tracking with **unique, high-feature images of known physical size** (e.g. 15×15 cm). Use ARCore Augmented Images / ARKit image detection, or AR Foundation's `ARTrackedImageManager`.
- The app must **generate printable marker sheets (PDF)** for a machine: a unique image per marker, the marker ID and machine name printed underneath, and a scale bar so the print size can be verified.
- The physical width is stored per marker and given to the tracker.

## 4. Users, roles and access control

| Role | Can do |
|---|---|
| **Admin** | Manage organisation, invite users, assign roles, create/delete machines, everything a Manager can do |
| **Manager** | Create machines, register markers, map/edit/delete parts, edit catalog data, assign machines to mechanics |
| **Mechanic** | Read-only: localise, view labels, open part details. Can add **maintenance notes/repair logs** to a part |

- **Enforce permissions on the backend, not just in the UI.** Use Supabase with **Row Level Security (RLS) policies** for every table, and write tests for the policies.
- Mechanics only see machines they are assigned to (via `machine_access`).
- The UI hides the mapping features for mechanics.
- Multi-tenant: everything is scoped to an `organization`.
- Login: Supabase Auth with email + password. Admins invite users by email.

## 5. Data model (Supabase / Postgres) — refine if needed

- `organizations (id, name, created_at)`
- `profiles (id = auth.users.id, organization_id, full_name, role enum[admin, manager, mechanic])`
- `machines (id, organization_id, name, model, serial_number, location, created_by, created_at)`
- `machine_access (user_id, machine_id)`
- `markers (id, machine_id, label, image_path, physical_width_m, pose_in_machine jsonb {position xyz, rotation quaternion xyzw}, is_origin bool, created_at)`
- `parts (id, machine_id, name, part_number, description, specs jsonb, position_in_machine jsonb {x,y,z}, manual_ref (doc + page), photo_path, created_by, updated_at)`
- `part_documents (id, part_id, storage_path, title, page)` for manual/catalog PDF pages
- `maintenance_logs (id, part_id, user_id, note, photo_path, created_at)`
- `audit_log (id, organization_id, user_id, action, entity, entity_id, diff jsonb, created_at)`

Use Supabase Storage for marker images, part photos and manual PDFs. Store all poses in metres.

## 6. App screens

1. **Login / invite acceptance**
2. **Machine list** (filtered by access)
3. **Machine detail:** markers, parts list (searchable by name/part number), "Print markers" action (Manager+)
4. **AR Lookup (all roles):**
   - "Scan a marker" guidance, then show the localised status
   - Pins/labels for parts in view, **filtered by distance** (configurable) to avoid clutter
   - Small dots that expand to a label when tapped or near the screen centre
   - Tap a label to open the part detail sheet
5. **AR Mapping (Manager+):**
   - Register origin marker / additional markers
   - Tap on a part, see a crosshair preview of the hit point, open a form (name, part number, description, specs, manual reference, optional photo captured from the camera), and save
   - Move or delete an existing part pin
6. **Part detail:** all data, linked manual pages, maintenance history, "Add note" (mechanic)
7. **Admin:** users, roles, machine assignments

## 7. Non-functional requirements

- **Offline-first for lookup:** cache machines, markers and parts locally so lookup works on factory floors with bad signal. Sync when back online. Mapping may require a connection in v1.
- Clean architecture: separate AR/pose math, data/repository layer and UI. **Unit-test the transform math** thoroughly (round trips, multi-marker consistency).
- No secrets in the repo; use `.env` / local config, with an `.env.example`.
- Create a `README.md` (setup, Supabase setup, building to a device, printing markers) and a `CLAUDE.md` with project conventions.
- Supabase schema as SQL migrations in the repo (tables, enums, RLS policies, storage buckets, seed data for a demo org with one user per role).
- Use git and commit after each completed phase with clear messages.

## 8. Tech stack (confirm with me first)

- **Android only:** Kotlin + Jetpack Compose + ARCore (Augmented Images) with SceneView for rendering, plus supabase-kt.
- **Android + iPhone:** Unity (LTS) + AR Foundation (ARCore + ARKit) + the Supabase C# client. Alternatively, propose a better cross-platform option if you have strong reasons.
- **Backend:** Supabase (Auth, Postgres + RLS, Storage).

## 9. Phases

1. **Foundation:** project scaffold, Supabase schema + RLS + seed, auth, roles, machine list
2. **AR core:** marker detection, origin localisation, pose math + unit tests, debug overlay that shows the axes
3. **Mapping mode:** tap-to-place parts, part form, save/edit/delete, multi-marker registration
4. **Lookup mode:** labels, distance filter, part detail, drift correction
5. **Markers PDF generator, offline cache, maintenance logs, audit log**
6. **Polish:** UX for gloves/outdoor light (big touch targets, high contrast), error states, README
7. **(Later)** Manual PDF ingestion + RAG "ask about this part" assistant

## 10. Open questions — ask me these before planning

1. Do the users have Android, iPhone, or both? (This decides the stack.)
2. Do I already have a Supabase project, or should you guide me through creating one?
3. What format are the existing catalog/manual in (PDF, Excel, a database)? Should v1 include importing catalog data from a file (e.g. CSV/Excel) so parts can be picked from the catalog instead of typed?
4. Roughly how big are the machines, and are they indoor or outdoor?
5. Which test phone(s) do I have available?

Start now: ask me the open questions, then present the plan.
