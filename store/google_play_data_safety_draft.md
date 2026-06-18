# Google Play Data Safety Draft — Local-first v1

Use this only if the signed binary matches the stated behavior.

## Core posture

- User photos/stickers: processed locally by default.
- No sale of user data.
- No keystroke collection from keyboard/IME.
- No location collection.
- No contacts collection.
- No account required for first sticker creation.

## Data types likely involved

| Data type | Collected? | Shared? | Purpose |
|---|---:|---:|---|
| Photos/videos | Only user-selected local processing | No, unless user shares manually | Sticker creation |
| App activity | Optional if analytics enabled | Depends on SDK | Product analytics/crash diagnosis |
| Device/app info | Optional crash logs | Depends on SDK | Reliability |
| Purchase history | If IAP/subscriptions enabled | Store processor | Monetization |

## Review rule

Before submission, inspect the final binary dependencies. If Firebase/RevenueCat/Sentry/analytics/ads SDKs exist, Data Safety must be updated to match actual behavior.
