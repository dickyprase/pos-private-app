# KopiPOS Native Android

Native Kotlin + Jetpack Compose cashier client for KopiPOS Laravel API. No WebView, PWA, or Android Print Framework.

## Build

```bash
./gradlew assembleStagingDebug
./gradlew assembleProductionDebug
```

Set production API URL in `app/build.gradle.kts` before release. Do not commit secrets or signing credentials.

## Current API contract

- `POST /api/auth/token`
- `GET /api/products`
- `GET /api/categories`
- `GET /api/shifts/active`
- `POST /api/orders`
- `GET /api/orders/{id}/receipt/raw`

Server remains source of truth for price, tax, stock, payment, and order idempotency.

## Status

Phase 1 native shell, auth, catalog, cart, and checkout client scaffolded. Bluetooth RFCOMM manager, receipt fetch/print integration, transaction history, and production API URL remain to be completed after build verification.
