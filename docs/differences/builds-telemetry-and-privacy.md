# Builds, telemetry, and privacy

Katari releases a standard build and a FOSS build. Both contain the same core library and media features, but they differ in application identity and telemetry components.

## Standard build

The standard release uses application ID `app.katari`. Official release builds include Firebase telemetry support and the in-app updater.

When telemetry is included, **More → Settings → Security and privacy** exposes separate controls for:

- Crash reporting
- Analytics

The onboarding permission step also presents these choices. Disabling them updates the telemetry SDK state.

## FOSS build

The FOSS release uses application ID `app.katari.foss`, so Android treats it as a separate application. It is built without the Firebase telemetry dependencies and uses no-op telemetry integration. Telemetry settings are therefore not shown.

Because the application IDs differ, standard and FOSS builds do not update one another in place and do not automatically share app-private data. Use a backup when moving between them. Their storage can be pointed at the same user-selected folder only with the same cautions that apply to two apps managing one storage location.

## APK variants

Official standard releases include a universal APK and ABI-specific APKs for supported processor architectures. The universal APK is the safest choice when unsure. The official FOSS artifact is universal.

Development, preview, and benchmark variants use separate application-ID suffixes and are not normal user releases.

## Privacy boundaries

Extensions and source websites make their own network requests and may have policies separate from Katari. Disabling Katari analytics does not prevent a website from observing requests sent to it. Backups can also contain sensitive source settings when private settings are selected.

Katari does not host content. Review extension trust, avoid sharing logs containing tokens or signed media URLs, and store backups securely.
