# Frontend — Angular app

Angular 20 (standalone components, signals, strict TypeScript) · Angular Material 20

## Run

The backend must be running on port 8080 first (see `../backend/README.md`).

```bash
npm ci        # first time only: installs the exact versions from package-lock.json
npm start     # http://localhost:4200
```

`npm start` runs `ng serve`, which uses `proxy.conf.json` to forward every `/api` request to
`http://localhost:8080`. The browser therefore talks to one origin and the backend needs no
CORS configuration.

## Test

```bash
npm test          # Karma + Jasmine in Chrome, watches for changes
npm run test:ci   # single run in headless Chrome (for CI / servers)
```

`test:ci` uses the `ChromeHeadlessCI` launcher from `karma.conf.js`, which adds
`--no-sandbox` for Linux servers where Chrome's sandbox is not available. If Chrome is not
on the `PATH`, set `CHROME_BIN` to the Chrome/Chromium executable.

## Dependency audit

`npm audit` reports high-severity findings in the **Karma test runner chain** (`karma` →
`chokidar` → `braces`). These packages are development-only and never reach the browser:
`npm audit --omit=dev` reports **0 vulnerabilities** for the shipped app. Karma is deprecated
upstream, so moving the tests to Angular's newer test runner is a natural follow-up.

## Build

```bash
npm run build     # production build in dist/frontend
```

## Structure

```
src/app
├── app.ts, app.html, app.scss   shell: toolbar navigation + <router-outlet>
├── app.config.ts                providers: router, HttpClient
├── app.routes.ts                /employees and /insights (lazy-loaded)
├── employees/                   → employees/README.md
├── insights/                    → insights/README.md
└── shared/                      → shared/README.md
```

Styles: `src/styles.scss` defines the Material 3 theme (azure palette) and the error
snackbar colours; each component has its own `.scss` file using Material's CSS variables
(`--mat-sys-*`), so colours stay consistent with the theme.
