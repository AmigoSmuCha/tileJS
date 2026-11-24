# tileJS

Three quick-settings tiles for the Unihertz Jelly Star:

- **Full charge** — raises the charge limit once (to 95% by default), then puts
  the 80% limit back when it reaches the target or drops back to 81%. Uses the
  phone's own `agui_common` service (no root). Whether a normal app may reach
  that service depends on the phone; the app says if it can't.
- **Front light** — the front RGB light on or off (system settings).
- **Back LEDs** — off / on / scheduled; the schedule times are set in the app.

The lights need the "Modify system settings" permission, granted in the app.
Every push builds `tileJS.apk` and publishes it under Releases.
