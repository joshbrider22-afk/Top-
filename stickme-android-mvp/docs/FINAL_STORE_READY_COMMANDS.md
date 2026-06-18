# StickMe Final Store-Ready Commands

Run from:

```bash
cd stickme-android-mvp
```

## 1. App Store Experience Gate tests

```bash
npm run test:app-store-gate
```

## 2. App Store Experience Gate preflight

```bash
npm run preflight:app-store-gate
```

## 3. Combined store gate

```bash
npm run store:ready
```

## 4. Android debug build

```bash
gradle :app:assembleDebug --stacktrace
```

## 5. Final combined command

```bash
npm run final:store-ready
```

## 6. Physical Android QA

- Install debug APK.
- Create demo sticker.
- Enable StickMe Keyboard.
- Open Google Messages.
- Tap sticker.
- Repeat in WhatsApp, Telegram, and Signal.
- Confirm fallback text appears where image insertion is unsupported.

## 7. Release gate

Submit only when:

```text
App Store Experience Gate = passed
Production Launch Lock = enabled
Maintenance Mode = off
Remote Store Review Mode = off
Privacy URL = public HTTPS
Terms URL = public HTTPS
Support URL = public HTTPS
Account deletion URL = public HTTPS
```
