# `shared` — building blocks used by several pages

| File | What it does | Why it exists |
|---|---|---|
| `api.ts` | `API_BASE_URL = '/api'` | One place for the API prefix (the dev proxy maps it to the backend). |
| `error-message.ts` | `errorMessage(error)` turns any HTTP error into one readable sentence (uses the backend's `detail`; explains when the server is unreachable). `fieldErrors(error)` returns the backend's `field → message` map. | Every page shows errors the same way; forms can put messages under the right input. |
| `dates.ts` | `toIsoDate(date)` / `parseIsoDate(text)` convert between a `Date` and `YYYY-MM-DD` using the **local** calendar day. | `Date.toISOString()` converts to UTC and can shift a hire date by one day. |
| `notification.service.ts` | `NotificationService.success()` / `.error()` show Material snackbars (errors in the error colour, shown longer). | Consistent feedback after save / delete. |
| `confirm-dialog/` | `ConfirmDialog` — "Are you sure?" dialog that closes with `true` when confirmed. Focus starts on **Cancel**. | Destructive actions (delete) always ask first; pressing Enter by accident does not delete. |
| `bar-chart/` | `BarChart` — horizontal bars drawn with CSS; each bar's length is relative to the largest value. Accessible labels for screen readers. | The dashboard needs simple bars only, so no chart library is added. |

## Tests

`error-message.spec.ts`, `dates.spec.ts`, `bar-chart/bar-chart.spec.ts`.
