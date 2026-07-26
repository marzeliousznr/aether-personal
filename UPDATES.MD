# Updating Aether

This app **never updates itself**. The auto-update mechanism has been removed
for security. All updates require explicit human review before installation.

## Check for Updates

```bash
# Quick check — makes zero local changes
scripts/check-upstream.sh

# Or manually:
git fetch origin
git log HEAD..origin/main --oneline
git diff HEAD..origin/main
```

## Apply Updates

After reviewing the diff:

```bash
git merge origin/main
# or, cherry-pick specific commits:
git cherry-pick <sha>
```

## Rebuild

```bash
./gradlew clean build
```

## Security Notes

- The in-game update checker fetches the latest release tag from
  `https://api.github.com/repos/marzeliousznr/aether-personal/releases/latest`
  and shows a chat notification with a link. It never downloads or installs
  anything automatically.
- All network endpoints are documented in the security audit. The only
  outbound connections are:
  - GitHub API (update check)
  - Hypixel CDN (resource pack fallback)
  - Coflnet API (bazaar pricing)
  - Elitebot API (feast timers)
  - Discord API (opt-in remote control, requires your own bot token)
- If you add new outbound connections, document them here and run a
  security re-audit.
