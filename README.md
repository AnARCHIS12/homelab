<p align="center">
  <img src="media-docs/app-icon.png" width="128" height="128" alt="Homelab Logo" />
</p>

# Homelab

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Made with Jetpack Compose](https://img.shields.io/badge/Made%20with-Jetpack%20Compose-green.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Homelab is a native Android application for monitoring and managing your self-hosted infrastructure from one place. Built with modern Android architecture, Kotlin, and Jetpack Compose (Material 3), it provides a fluid, secure, and intuitive dashboard for your servers, containers, networks, and services.

---

## Project Revival & Maintenance

We are pleased to announce that **Homelab is back!** The project has been fully resumed and the application is now **actively maintained on a daily basis**.

However, we are officially **discontinuing and abandoning all iOS support**. We are firmly opposed to Apple's closed-source, restrictive ecosystem and walled-garden policies. Moving forward, 100% of our focus and development effort is dedicated to **Android** as an open, modern, and free platform for self-hosters.

---

## 100% Free & No Donations

Homelab is and will always remain **100% free and open-source**.

There will **never be any donations**, paid tiers, subscriptions, telemetry, or advertisements. We do not accept and will never ask for any donations. The project is created strictly for the self-hosting community.

---

<p align="center">
  <img src="media-docs/foto-android/Dashboard.jpg" width="280" alt="Homelab Android Dashboard" />
</p>

---

## Highlights

- **34 integrated service dashboards** across infrastructure, networking, media automation, observability, and developer tooling.
- **One app, many instances**: add multiple instances of the same service and switch between them seamlessly.
- **100% Native Android**: built with Kotlin, Jetpack Compose, Material 3, dynamic theming, and Kotlin Coroutines/Flow.
- **Robust Security & Privacy**: hardware-backed Android KeyStore credential encryption (AES-256-GCM), PBKDF2 PIN protection, screenshot protection (`FLAG_SECURE`), and strict default HTTPS.
- **Practical daily-use features**: encrypted local backup and restore, biometric unlock (Fingerprint / Face Unlock), multilingual UI, and in-app update checks.
- **Utilities beyond services**: built-in bookmarks plus quick Tailscale launch support for remote access workflows.

---

## Integrated Services

### Core Infrastructure

- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/portainer.png" width="18" style="vertical-align:middle"> **Portainer**: container overview, quick actions, resource usage.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/proxmox.png" width="18" style="vertical-align:middle"> **Proxmox VE**: nodes, guests, storage, networking, backups, and cluster operations.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/truenas-core.png" width="18" style="vertical-align:middle"> <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/truenas-scale.png" width="18" style="vertical-align:middle"> **TrueNAS Scale / Core**: storage, pools, disks, shares, services, and system alerts.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/uptime-kuma.png" width="18" style="vertical-align:middle"> **Uptime Kuma**: monitor status, uptime visibility, and incident tracking.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/dockhand.png" width="18" style="vertical-align:middle"> **Dockhand**: native container management dashboard.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/dockmon.png" width="18" style="vertical-align:middle"> **DockMon**: Docker host and container monitoring with logs, restart, and update actions.
- <img src="media-docs/icons/komodo.png" width="18" style="vertical-align:middle"> **Komodo**: resource, deployment, stack, and server monitoring.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/beszel.png" width="18" style="vertical-align:middle"> **Beszel**: server monitoring across nodes.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/linux-update-dashboard.png" width="18" style="vertical-align:middle"> **Linux Update**: pending package updates across hosts.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/crafty-controller.png" width="18" style="vertical-align:middle"> **Crafty Controller**: game server management dashboard.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/pterodactyl.png" width="18" style="vertical-align:middle"> **Pterodactyl**: game server management panel with power controls, resource stats, and live status visibility.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/calagopus.png" width="18" style="vertical-align:middle"> **Calagopus**: next-generation game server management panel with power controls, uptime, and resource stats.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/gitea.png" width="18" style="vertical-align:middle"> **Gitea / Forgejo**: repositories, activity, and source browsing.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/pangolin.png" width="18" style="vertical-align:middle"> **Pangolin / Newt**: tunnel and peer visibility.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/healthchecks.png" width="18" style="vertical-align:middle"> **Healthchecks**: uptime checks and health status.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/patchmon.png" width="18" style="vertical-align:middle"> **PatchMon**: software update visibility across your stack.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/wakapi.png" width="18" style="vertical-align:middle"> **Wakapi**: coding activity and time tracking stats.

### Networking & DNS

- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/pi-hole.png" width="18" style="vertical-align:middle"> **Pi-hole**: queries, blocked domains, toggles, timers.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/adguard-home.png" width="18" style="vertical-align:middle"> **AdGuard Home**: filters, rewrites, blocked services, query activity.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/ubiquiti-unifi.png" width="18" style="vertical-align:middle"> **Ubiquiti Network**: gateways, switches, access points, clients, and site visibility.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/technitium.png" width="18" style="vertical-align:middle"> **Technitium DNS**: DNS metrics and health.
- <img src="media-docs/icons/maltrail.png" width="18" style="vertical-align:middle"> **Maltrail**: threat detections, daily findings, and event visibility.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/nginx-proxy-manager.png" width="18" style="vertical-align:middle"> **Nginx Proxy Manager / NPMplus**: proxy hosts, streams, redirects, certificates, access lists.

### Media & Observability

- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/plex.png" width="18" style="vertical-align:middle"> **Plex**: libraries, sessions, recently added media.
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/jellystat.png" width="18" style="vertical-align:middle"> **Jellystat**: Jellyfin activity, streams, usage insights.

### Servarr Stack

- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/sonarr.png" width="18" style="vertical-align:middle"> **Sonarr**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/radarr.png" width="18" style="vertical-align:middle"> **Radarr**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/lidarr.png" width="18" style="vertical-align:middle"> **Lidarr**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/prowlarr.png" width="18" style="vertical-align:middle"> **Prowlarr**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/qbittorrent.png" width="18" style="vertical-align:middle"> **qBittorrent**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/jellyseerr.png" width="18" style="vertical-align:middle"> **Jellyseerr**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/bazarr.png" width="18" style="vertical-align:middle"> **Bazarr**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/gluetun.png" width="18" style="vertical-align:middle"> **Gluetun**
- <img src="https://cdn.jsdelivr.net/gh/selfhst/icons/png/flaresolverr.png" width="18" style="vertical-align:middle"> **FlareSolverr**

The full Servarr stack is available as a unified media automation dashboard, so downloads, health, requests, VPN status, and indexer state can be checked from one place.

---

## Screenshots & Showcase

The Android app is built with **Kotlin** and **Jetpack Compose** for **Android 8.0+ (API 26+)**. It uses a modern Material 3 style, dynamic color theming, expressive cards, and native Android architecture patterns.

<table align="center">
  <tr>
    <th>Dashboard</th>
    <th>Servarr</th>
    <th>Bookmarks</th>
  </tr>
  <tr>
    <td align="center"><img src="media-docs/foto-android/Dashboard.jpg" width="180" /></td>
    <td align="center"><img src="media-docs/foto-android/Servarr.jpg" width="180" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_19_2026-03-16_20-24-21.jpg" width="180" /></td>
  </tr>
</table>

<table align="center">
  <tr>
    <td align="center"><img src="media-docs/foto-android/photo_1_2026-03-16_20-24-21.jpg" width="120" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_4_2026-03-16_20-24-21.jpg" width="120" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_13_2026-03-16_20-24-21.jpg" width="120" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_16_2026-03-16_20-24-21.jpg" width="120" /></td>
    <td align="center"><img src="media-docs/foto-android/plex.jpg" width="120" /></td>
  </tr>
  <tr>
    <td align="center"><sub>Portainer</sub></td>
    <td align="center"><sub>Beszel</sub></td>
    <td align="center"><sub>Nginx Proxy</sub></td>
    <td align="center"><sub>Pi-hole</sub></td>
    <td align="center"><sub>Plex</sub></td>
  </tr>
</table>

<details>
<summary><b>View all Android screenshots</b></summary>
<br>

**Portainer**
<table>
  <tr>
    <td align="center"><img src="media-docs/foto-android/photo_1_2026-03-16_20-24-21.jpg" width="180" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_2_2026-03-16_20-24-21.jpg" width="180" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_3_2026-03-16_20-24-21.jpg" width="180" /></td>
  </tr>
</table>

**Beszel**
<table>
  <tr>
    <td align="center"><img src="media-docs/foto-android/photo_4_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_5_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_6_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_7_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_8_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_9_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_10_2026-03-16_20-24-21.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_11_2026-03-16_20-24-21.jpg" width="110" /></td>
  </tr>
</table>

**Nginx Proxy Manager / NPMplus · Pi-hole**
<table>
  <tr>
    <td align="center"><img src="media-docs/foto-android/photo_13_2026-03-16_20-24-21.jpg" width="145" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_14_2026-03-16_20-24-21.jpg" width="145" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_16_2026-03-16_20-24-21.jpg" width="145" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_17_2026-03-16_20-24-21.jpg" width="145" /></td>
  </tr>
</table>

**AdGuard Home · Healthchecks · PatchMon · Jellystat · Plex**
<table>
  <tr>
    <td align="center"><img src="media-docs/foto-android/adguard1.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/adguard2.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/healthcheck1.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/healthcheck2.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_1_2026-03-21_01-00-34.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_2_2026-03-21_01-00-34.jpg" width="110" /></td>
    <td align="center"><img src="media-docs/foto-android/plex.jpg" width="110" /></td>
  </tr>
</table>

**Bookmarks**
<table>
  <tr>
    <td align="center"><img src="media-docs/foto-android/photo_18_2026-03-16_20-24-21.jpg" width="180" /></td>
    <td align="center"><img src="media-docs/foto-android/photo_19_2026-03-16_20-24-21.jpg" width="180" /></td>
  </tr>
</table>

</details>

---

## Installation

### Direct APK Download

Download the latest release APK directly from the Releases section:

1. Download `Homelab.apk` to your Android device.
2. Open and tap the downloaded file to install (grant the "Install unknown apps" permission if prompted).
3. Launch Homelab and configure your self-hosted instances.

---

## Development & Building

### Repository Structure

- `HomelabAndroid/`: Native Android application built with Kotlin, Jetpack Compose, and Material 3.
- `media-docs/`: Screenshots, icons, and documentation assets.
- `docs/`: Web pages and documentation.

### Build from Source

#### Prerequisites
- Android Studio Ladybug (or newer) / IntelliJ IDEA
- JDK 21
- Android SDK (API level 35, min API 26)

#### Command Line Build

```bash
cd HomelabAndroid

# Compile Kotlin code
./gradlew :app:compileDebugKotlin

# Run unit and security tests
./gradlew :app:testDebugUnitTest

# Assemble Debug APK
./gradlew :app:assembleDebug
```

The compiled APK will be generated at:
`HomelabAndroid/app/build/outputs/apk/debug/app-debug.apk`

---

## Support & Community

For questions, feature requests, or bug reports, please open an issue on GitHub.

- **Privacy Policy:** See [docs/privacy.html](docs/privacy.html)

---

## License

This project is licensed under the **Apache License 2.0**.

See [LICENSE](LICENSE) for the full text.
