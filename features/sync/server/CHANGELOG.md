# Sync Server Changelog

## [1.1.0] - 2026-11-12

> This version upgrades the database in `/data`, and 1.0.x can't open it afterwards, so copy the volume before upgrading if you might go back.

### ✨ Added

- Devices on an account can send each other messages. The server keeps a message until its device fetches it or for up to 7 days, and at most 1,000 waiting per device, dropping the oldest.

## [1.0.1] - 2026-11-11

### 🔄 Changed

- The image runs on Java 25 instead of Java 21.

## [1.0.0] - 2026-11-08

### ✨ Added

- The first visit to the server's web page creates the admin account.
- Admins add accounts, invite people with one-time codes, reset passwords, and disable or delete accounts on the web page.
- All data lives in one volume at `/data`, and deleted records and unused files are cleaned up on their own.
- `reset-password` gives a locked-out admin a new password from inside the container.

[Unreleased]: https://github.com/pa2x2/katari/compare/sync-server-v1.1.0...HEAD
[1.1.0]: https://github.com/pa2x2/katari/tree/sync-server-v1.1.0
[1.0.1]: https://github.com/pa2x2/katari/tree/sync-server-v1.0.1
[1.0.0]: https://github.com/pa2x2/katari/tree/sync-server-v1.0.0
