# GlobalPulse News — EGX + US Market News Awareness Dashboard

A free-tier, news-awareness dashboard for beginner investors, focused on the **Egyptian Exchange (EGX)** and **US markets (NYSE/NASDAQ)**. It surfaces trending, ticker-tagged financial news with sentiment context — it does **not** tell you to buy or sell.

## What this is (and isn't)

- **Is:** a tool to help you notice what news is moving around a company/market, with sentiment as *context*, not a signal.
- **Isn't:** a trading bot, a buy/sell recommendation engine, or a substitute for your own research. See [DISCLAIMER.md](./DISCLAIMER.md).

News-driven trading is genuinely hard — by the time a story is "trending," the price has often already moved. This tool is built to make you *better informed*, not to time entries and exits.

## Why this design

- **No paid APIs.** Runs entirely on providers with a usable free tier (see Data Sources below).
- **Provider abstraction.** News sources are pluggable (`app/providers/`), so a paid/licensed source can be swapped in later without touching the rest of the app.
- **Every item is sourced and timestamped.** No invented data, no fake companies, no unlabeled "delayed" data pretending to be live — consistent with the project's own data-integrity rules.
- **Beginner-first UI.** Sentiment scores are explained in plain language, not just shown as a number.

## Data sources (free tier)

| Source | Coverage | Free tier limit |
|---|---|---|
| [Marketaux](https://www.marketaux.com/) | Ticker-tagged financial news + sentiment (-1 to 1) | 100 requests/day, no card required |
| [Currents API](https://currentsapi.services/) | General world/local news (economy, politics, central banks) | ~600 requests/day, no card required — **verify current commercial-use terms before relying on this beyond personal use, providers change these** |

Egypt/EGX coverage on both is **not guaranteed** to be as deep as US coverage — this is flagged explicitly in the UI per item, not hidden. If a request returns no EGX results for a given day, the dashboard says so rather than showing nothing silently.

## Verification status and "potential impact" — what they actually are

Two features were added on request, and it matters what they are *not*:

- **Verification status** (`SINGLE_SOURCE` / `MULTI_SOURCE_VERIFIED`) counts how many distinct sources reported a similar headline (simple title-token overlap, see `app/verification.py`). It is **not** a fact-check or a truth score — it only ever claims "multiple outlets covered this," never "this is accurate." A `CONFLICTING_REPORTS` status exists in the data model for a future version that can actually detect contradicting figures between sources; today's clustering can't reliably do that, so it's never emitted rather than faked.
- **Potential impact** is a transparent, hand-written keyword→sector lookup table (`app/impact_rules.py`), not a trained prediction model. "Oil ↔ energy/airlines" is a documented rule you can read and edit, not an AI forecast. Building an actual predictive model needs historical price data, backtesting, and calibration monitoring — that's real ML work, not a feature you bolt onto a news feed for free.

Both are labeled as such in the dashboard UI. Do not relabel either of these as "AI-verified" or "AI-predicted" in any UI copy — that would overstate what's actually happening and could give false confidence for real financial decisions.

Adding another provider later (e.g. a licensed one) means writing one new file in `app/providers/` that implements the same interface as `base.py` — nothing else changes.

## Setup

```bash
git clone <this-repo>
cd globalpulse-news
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
# edit .env and paste your free Marketaux API token (get one at marketaux.com — no card needed)
uvicorn app.main:app --reload
```

Then open `http://127.0.0.1:8000` in your browser.

## Project structure

```
app/
  main.py              FastAPI app, serves API + static dashboard
  config.py            Loads settings from .env
  models.py            Pydantic schemas for news items
  tickers.py           Starter ticker/company lists for US + EGX (see note below)
  cache.py             Simple file-based cache (respects the 100 req/day limit)
  providers/
    base.py            NewsProvider interface — implement this to add a new source
    marketaux.py        Marketaux implementation
  services/
    news_service.py     Fetches, caches, and merges news across configured markets
static/
  index.html           The dashboard (vanilla HTML/CSS/JS, no build step)
tests/
  test_news_service.py  Unit tests using a fake provider (no network needed)
```

## About the EGX ticker list

`app/tickers.py` ships with a **small starter set** of long-established, well-known EGX-listed companies (e.g. Commercial International Bank, Telecom Egypt, Talaat Moustafa Group). This is deliberately *not* presented as "the current EGX30" — index membership is reviewed twice a year by EGX itself. Before relying on this list, cross-check it against the official constituent list at [egx.com.eg](https://www.egx.com.eg) and update `app/tickers.py` accordingly. The code treats this list as configuration, not as verified fact.

## Rate-limit reality

100 requests/day is not much. The app caches every response (`app/cache.py`) and the dashboard has a manual "Refresh" action rather than auto-polling, so you don't burn the quota on page reloads. If you outgrow this, the fix is a second provider in `app/providers/`, not a rewrite.

## Not built yet (by design — this is an MVP)

- No trading execution, no brokerage integration, no auto buy/sell logic — intentionally out of scope.
- No GCC markets (Tadawul/ADX/DFM/QSE) — discussed and deliberately deferred, since no free, reliably-licensed source exists for them today.

## Android app (native, Clean Architecture)

`android/` is a real native Android client — Kotlin, Jetpack Compose, MVVM, Retrofit + kotlinx.serialization — not a WebView wrapper. It calls the backend's `/api/news` JSON endpoint directly and renders its own UI (market tabs, sentiment dots, verification badges, potential-impact lines), matching the web dashboard's dark/gold aesthetic.

```
android/app/src/main/java/com/globalpulse/news/
  domain/            Pure Kotlin models + repository interface (no Android/network deps)
  data/
    remote/          Retrofit API + DTOs matching the backend's exact JSON shape
    repository/      Repository impl + DTO->domain mapper
    di/              Manual dependency provisioning (no Hilt — see ServiceLocator.kt)
  presentation/
    news/            ViewModel, UI state, Compose screen + components
    theme/            Compose color scheme / typography
```

**Before this works, the backend must be hosted somewhere public** (Render, Railway, Fly.io, etc.) — a phone cannot reach `localhost` on your computer. Once it is:

1. Edit `android/gradle.properties` and replace `BASE_URL` with your real `https://` backend URL (must end in `/`). This flows into `BuildConfig.BASE_URL` at build time — no Kotlin code to touch.
2. Commit and push to `master`/`main`.
3. GitHub Actions (`.github/workflows/android-build.yml`) runs the full unit test suite (`gradle test`), then builds the APK (`gradle assembleDebug`) — the build fails if tests fail, so a broken commit never produces a "working" APK.
4. On GitHub → your repo → **Actions** tab → the latest workflow run → **Artifacts** → download `globalpulse-news-debug-apk` (contains `app-debug.apk`) and `unit-test-results` (readable test reports).
5. Transfer the APK to your phone and install it (Android will warn about "install unknown apps" since this isn't from Google Play — expected for a debug build).

### Tests included

- `NewsMapperTest` — DTO→domain mapping, including malformed dates and unknown enum values from the backend (never crashes, falls back safely).
- `NewsRepositoryImplTest` — uses MockWebServer to exercise the real Retrofit + serialization pipeline against JSON shaped exactly like the live backend, including error responses (503, malformed JSON).
- `NewsViewModelTest` — state transitions (loading → success/error), market switching, redundant-fetch avoidance, refresh — using a fake repository and `kotlinx-coroutines-test`.

These are unit tests (`app/src/test/`), runnable with `gradle test` — no emulator or device needed, and CI runs them on every push. A `androidTest/` UI-test source set is wired into the Gradle config (Compose UI testing dependencies are already there) but no instrumented tests are written yet — that needs an emulator/device in CI, which is a further step, not done here.

### Honesty about what was and wasn't verified

This was built and reviewed carefully — package structure, constructor signatures, and imports were cross-checked by hand, and every XML resource was validated. **It has not been compiled or run**, because this development environment has no Android SDK, no Gradle, and no network access to fetch either — that's a real constraint of the sandbox this was built in, not a shortcut taken. The real first compile happens in GitHub Actions on your first push. If it fails, copy the error from the Actions log back — a first-compile error in a project this size (undeclared import, a Compose API signature that shifted between library versions) is normal and fast to fix once there's an actual error message to work from, not a guess.

This is a **debug** build (unsigned, not optimized) — fine for testing on your own device. Publishing to Google Play needs a signed release build, which is a separate step (a signing key, `assembleRelease`, and Play Console setup) — not done here yet.* build (unsigned, not optimized) — fine for testing on your own device. Publishing to Google Play needs a signed release build, which is a separate step (a signing key, `assembleRelease`, and Play Console setup) — not done here yet.

