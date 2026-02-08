# ShieldDNS - Android DNS-Based Ad Blocker

🛡️ DNS-level ad blocking for Android without root access.

## Features

- **One-tap VPN protection** - Enable/disable with a single tap
- **DNS-based blocking** - Blocks ads and trackers at DNS level
- **Privacy-focused** - No HTTPS inspection, no data collection
- **Built-in blocklist** - Common ad networks pre-configured
- **Material 3 UI** - Modern, beautiful interface

## How It Works

ShieldDNS creates a local VPN to intercept DNS queries. When an app requests `ads.example.com`, ShieldDNS checks against its blocklist and returns `0.0.0.0` for blocked domains, preventing the ad from loading.

```
App → DNS Query → ShieldDNS VPN → Blocklist Check
                                    ↓
                    [BLOCKED] → Return 0.0.0.0
                    [ALLOWED] → Forward to upstream DNS
```

## Limitations

> ⚠️ **Important**: DNS-level blocking cannot block:
> - YouTube server-side ads (served from same domain as video)
> - In-app ads over HTTPS (encrypted traffic)
> - Ads using direct IP addresses

## Tech Stack

- **Kotlin** - 100%
- **Jetpack Compose** - Modern declarative UI
- **Hilt** - Dependency injection
- **Room** - Local database
- **Coroutines + Flow** - Async operations
- **Material 3** - Design system

## Architecture

```
├── domain/           # Business logic, models
├── data/             # Repositories, database
├── service/          # VPN, DNS parsing, filtering
└── presentation/     # UI (Compose screens)
```

## Building

```bash
./gradlew assembleDebug
```

## License

MIT License

---

Made with ❤️ for a better browsing experience
