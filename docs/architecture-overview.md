# Architecture Overview

This document provides a high-level architecture overview for the StickMe Android project (stickme-android-mvp) and its CI gates.

```mermaid
flowchart LR
  subgraph GH[GitHub]
    Dev[Developer (push / pull request)] --> Repo[Repository: joshbrider22-afk/Joshua-Brider]
    Repo --> Workflows[GitHub Actions Workflows]
  end

  subgraph CI[GitHub Actions Runners]
    Workflows --> AndroidCI[stickme-android-ci.yml]
    Workflows --> StoreGateCI[stickme-store-gate-ci.yml]

    AndroidCI --> BuildJob[build-debug job]
    BuildJob --> Gradle[Gradle (8.10.2) / Java 17]
    Gradle --> Assemble[assembleDebug]
    Assemble --> DebugAPK[Debug APK Artifact]
    DebugAPK --> Artifacts[GitHub Artifacts]

    StoreGateCI --> NodeJob[store-gate job]
    NodeJob --> NodeSetup[Node (22) / npm]
    NodeSetup --> Install[npm install]
    Install --> Tests[Run app store gate tests]
    Install --> Preflight[Run preflight checks]
    Tests --> TestResults[Tests & Reports]
    Preflight --> PreflightResults[Preflight Output]
  end

  subgraph App[Android App: stickme-android-mvp]
    AppModule[app module
(Gradle Android app)]
    Scripts[scripts / toolchains]
    SupabaseCfg[supabase config & helpers]
    AppModule --> Assemble
    Scripts --> Preflight
    SupabaseCfg --> StoreGateCI
    SupabaseCfg --> NodeJob
  end

  subgraph Services[External Services]
    Supabase[Supabase backend]
    AppStore[App Stores (Google Play / App Store)]
  end

  DebugAPK --> AppStore
  TestResults --> AppStore
  Supabase --> AppModule

  classDef repo fill:#f8f9fa,stroke:#333,stroke-width:1px;
  classDef ci fill:#eef6ff,stroke:#2b7cff;
  classDef app fill:#fff7e6,stroke:#ff8c00;
  classDef svc fill:#e6fff2,stroke:#06b981;

  class GH repo;
  class CI ci;
  class App app;
  class Services svc;
```

Notes:

- The stickme-android-ci workflow builds the Android app using Gradle and produces a debug APK artifact that is uploaded to GitHub artifacts.
- The stickme-store-gate-ci workflow runs Node-based tests and preflight checks (app store gate) using npm; these validate store-readiness and integration with Supabase configuration.
- External services like Supabase provide backend functionality used by the app and are referenced by store-gate tests and preflight scripts.

Suggested next steps:

- Add links to CI badge(s) and update this diagram if you add release or publish workflows.
- Expand the Services section with any additional backends or third-party APIs the app depends on.
