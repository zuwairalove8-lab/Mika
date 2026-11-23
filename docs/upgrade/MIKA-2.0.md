# Mika 2.0 — audit and plan

This page records what the repository already does, what this pass changed, and what is
still left to do. It is meant to be kept up to date as the upgrade proceeds; every claim
below points at real code, not at intentions.

## What already exists (verified by reading the code)

The brief describes most of the intelligence system as if it were missing. It is not.
Before adding anything, these were confirmed present:

| Brief | Existing implementation |
| --- | --- |
| Observe → plan → act → verify loop | `core/agent/AgentLoop.kt`, `core/agent/SystemPrompt.kt`, `core/verify/{Verifier,EvidenceLedger,Grounding,AnswerSources}.kt` |
| Model routing | `core/agent/ModelRouter.kt` (main / light split, trouble fallback), `core/agent/ModelCaller.kt` |
| Capability detection | `core/model/Capabilities.kt` (`toolCalls`, `vision`, `contextTokens`, `reasoning`, `groundingVerified`, `maxImages`) |
| Multi-provider support | `core/model/{AnthropicModel,GeminiModel,OpenAiCompatModel,ResponsesModel,ModelCatalog}.kt` |
| Tool registry and risk gating | `core/tool/{ToolDispatcher,ToolContext,AgentTool}.kt`, `core/risk/`, `core/gate/` |
| Browser page understanding | `core/cap/web/{PageDigest,PageTree,ElementFusion,ElementProbe,SelectorPolicy,ReadScript}.kt` |
| Browser settling and pacing | `core/cap/web/{SettlePolicy,PagePacing,PageDwell,NetworkLedger}.kt` |
| Browser runtime | `core/cap/web/cdp/` (CDP transport, input, double-click), `app/.../cap/web/` |
| Skills | `core/skills/{PersonalSkills,SkillDrafter,SkillImport,SkillOutcomes,SkillRedactor}.kt` + `app/.../ui/SkillsSection.kt` |
| Memory | `core/memory/`, `app/.../ui/MemoryViewModel.kt`, `app/.../memory/` |
| Autonomous scheduling | `app/.../task/{TriggerRuntime,FileTriggerStore,TaskRun}.kt` |
| Child agents | `core/child/`, `app/.../task/{TaskRun,FileChildRunStore}.kt` |
| Vault (logins, wallets) | `core/vault/`, `app/.../vault/VaultStore.kt` |
| Task state / dashboard data | `app/.../task/TaskCenterState.kt`, `app/.../ui/RunTimeline*` |

So the useful work is not re-implementing these, but closing the gaps between them.

## Gaps found

1. **Skills marketplace was a mock.** `Route.SkillsMarketplace` rendered a screen with a
   hardcoded list, an “Add” button with an empty `onClick`, a “Browse file” button with an
   empty `onClick`, an import action that always reported success without importing, and a
   static “no skills yet” box — while a complete skill store, draft review and import path
   already existed in `SkillsSection.kt` / `MemoryViewModel`. The route was also unreachable
   from anywhere in the app.
2. **No URL import for skills.** The brief asks for installable skills from any source;
   only pasted text and a local file could be imported.
3. Remaining brief items not addressed in this pass are listed under “Next phases”.

## What this pass changed

- `core/skills/SkillSource.kt` — fetches a shared `SKILL.md` from a URL. http/https only,
  GitHub `blob`/`raw` links rewritten to `raw.githubusercontent.com`, content-type checks,
  256 KB hard cap, empty-body rejection, distinct reason codes. Format and safety
  validation stays in `SkillImport.prepare`, so the URL path cannot bypass what a pasted
  skill goes through.
- `core/skills/SkillSourceTest.kt` — covers fetch/strip of BOM, URL rewriting, scheme
  rejection (no request sent), HTTP error, non-text payload, oversized body, empty body,
  and markdown content type.
- `MemoryViewModel.importSkillFromUrl` — fetches, then reuses `importSkill` so an imported
  skill still lands in “To review” and needs an explicit accept.
- `SkillsSection` — the import dialog now takes a URL (with a Fetch button and mapping of
  failures to messages), alongside the existing file picker and paste box, and can be
  opened directly (`startImport`).
- `SkillsMarketplaceScreen` — rewritten as a real hub: installed skills with enable/disable,
  “To review” drafts, “Built-in” read from `GuideCatalog.loadBundled()`, and import. No
  hardcoded fake rows, no dead buttons.
- `MainActivity` — one `rememberMemoryViewModel` used by both the memory screen and the
  skills hub, so both views observe the same store.
- `SettingsScreen` — a “Skills Marketplace” row so the hub is actually reachable.

## Next phases

Ordered by value per unit of risk. Each phase should ship with tests and be verified by the
`Android build` workflow (tests, lint, assembleDebug) before merging.

1. **Browser telemetry (brief §4).** Add counters for navigation, settle and element
   discovery time in `SettlePolicy`/`PageDwell`, surface them in run records. Measure first;
   only then replace the remaining fixed waits (`CdpInput` step delay, `CdpDoubleClick` gap —
   both intentional, keep them until a measurement says otherwise).
2. **Skill permissions (brief §6).** Skills currently validate tool names but do not declare
   per-skill permissions. Extend `SkillDrafter.validate` with a required-permissions block and
   gate execution in `ToolDispatcher`.
3. **Memory retrieval scoring (brief §7).** `core/memory/MemoryIndex` exists; add usage-based
   relevance so retrieval prefers skills and notes that actually succeeded recently
   (`SkillOutcomes` already tracks this per skill).
4. **Task dashboard (brief §11).** `TaskCenterState` has the data; add a screen listing
   active/failed/completed runs with stage and verification outcome.
5. **Model routing by task kind (brief §2E).** `ModelRouter` picks main vs light per step;
   extend it to code/web/vision roles only after `Capabilities` reports them honestly for
   every provider in `ModelCatalog`.
6. **Reasoning budget (§2C).** Depth is currently implied by step budget; a measured
   complexity signal would need prompt-surface goldens updated alongside it
   (`core/src/test/resources/prompt-surface`).
