# KSR-OITC (One In The Chamber)

Ein schnelles, kompetitives **One-In-The-Chamber**-Minigame für Spigot/Paper **1.21+**.  
Mehrere Arenen, Auto-Countdown, Zuschauer-Modus, Punktesystem-Hook, Bossbar/Scoreboard, Join-Schilder – alles drin.

---

## Inhalt

- [Was ist OITC? (Spielprinzip)](#was-ist-oitc-spielprinzip)
- [Haupt-Features](#haupt-features)
- [Voraussetzungen](#voraussetzungen)
- [Installation](#installation)
- [Schnellstart](#schnellstart)
- [Arenen anlegen & verwalten](#arenen-anlegen--verwalten)
- [Match-Ablauf](#match-ablauf)
- [Zuschauer (Spectator)](#zuschauer-spectator)
- [Kommandos](#kommandos)
- [Berechtigungen (Permissions)](#berechtigungen-permissions)
- [Konfiguration](#konfiguration)
- [Schilder, Scoreboard & Bossbar](#schilder-scoreboard--bossbar)
- [Punkte-Integration (optional)](#punkte-integration-optional)
- [Daten & Persistenz](#daten--persistenz)
- [Entwickler-Notizen (Architektur)](#entwickler-notizen-architektur)
- [FAQ / Troubleshooting](#faq--troubleshooting)
- [Lizenz](#lizenz)

---

## Was ist OITC? (Spielprinzip)

**One In The Chamber** ist ein Arena-Modus mit extrem kurzen Time-to-Kill:

- Jeder Spieler startet mit:
    - **Bogen + 1 Pfeil**
    - **Steinschwert** (leicht reduzierter Schaden)
- **One-Hit-Kill:** Jeder Treffer (Pfeil) tötet sofort (Schwert normal).
- **Belohnungspfeil:** Tötest du **mit einem Pfeil**, bekommst du **sofort 1 neuen Pfeil**.
- **Respawn:** Nach dem Tod spawnst du an einem zufälligen Arena-Spawn.
- **Void-Sonderfall:** Fällst du **ohne Pfeil** ins Void, respawnst du **mit Bogen & Schwert, aber ohne Pfeil**.
- **Ziel:** Erreiche als Erster die konfigurierte Kill-Zahl **oder** habe nach Ablauf der Zeit die meisten Kills.

---

## Haupt-Features

- ✅ **Mehrere Arenen** parallel
- ✅ **Auto-Countdown** bei genügend Spielern
- ✅ **Zuschauer-Modus** (werden **nicht** mitgezählt)
- ✅ **Bossbar + Scoreboard**
- ✅ **Join-Schilder** mit Live-Status
- ✅ **Persistenz & Safestop**
- ✅ **RankPoints-Integration (optional)**

---

## Voraussetzungen

- **Paper/Spigot 1.21+**
- **Java 17+**
- (Optional) **RankPoints API** Plugin

---

## Installation

1. Plugin-JAR in den Ordner `plugins/` legen.
2. Server starten, damit Standard-Konfiguration erzeugt wird.
3. Arenen anlegen (siehe unten).
4. Losspielen! 🎯

---

## Schnellstart

```bash
/oitc setlobby
/oitc addspawn <arena>
/oitc join <arena>
```

Startet automatisch, sobald genug Spieler vorhanden sind.  
Manuell starten: `/oitc start <arena>`

---

## Arenen anlegen & verwalten

Eine Arena besteht aus:
- **Weltname**, **Lobby**, **mind. zwei Spawns**
- Parametern wie **minPlayers**, **maxPlayers**, **maxKills**, **maxSeconds**

**Spawns verwalten:**
```bash
/oitc addspawn <arena>
/oitc listspawns <arena>
/oitc clearspawns <arena>
```

---

## Match-Ablauf

- Lobby → Countdown → Running → End
- Countdown startet automatisch, wenn genug Spieler da sind.
- Abbruch, wenn zu wenige Spieler übrig sind.
- Sieg bei **maxKills** oder **Zeitablauf** (höchste Kills gewinnt).
- Zuschauer zählen **nicht** als Spieler.

---

## Zuschauer (Spectator)

- **Join während RUNNING** = automatisch Spectator.
- Unsichtbar, unverwundbar, keine Interaktion.
- Zählen **nicht** für Mindestspielerzahl.

---

## Kommandos

| Befehl | Beschreibung | Permission |
|---|---|---|
| `/oitc join <arena>` | Arena beitreten | `oitc.use` |
| `/oitc leave` | Arena verlassen | `oitc.use` |
| `/oitc start <arena>` | Match starten | `oitc.admin` |
| `/oitc reset <arena>` | Arena zurücksetzen | `oitc.admin` |
| `/oitc reset all` | Alle Arenen resetten | `oitc.admin` |
| `/oitc setlobby` | Main-Lobby setzen | `oitc.admin` |
| `/oitc addspawn <arena>` | Spawn hinzufügen | `oitc.admin` |
| `/oitc clearspawns <arena>` | Spawns löschen | `oitc.admin` |
| `/oitc listspawns <arena>` | Spawns anzeigen | `oitc.admin` |
| `/oitc reload` | Config neu laden | `oitc.admin` |

---

## Berechtigungen (Permissions)

```yaml
oitc.use:    # Standard-Spieler
  default: true

oitc.admin:  # Adminbefehle
  default: op
```

---

## Konfiguration

```yaml
countdown_seconds: 15
signs:
  update_interval_ticks: 20
  allow_spectate_on_running: true
```

---

## Schilder, Scoreboard & Bossbar

- **Join-Schilder:** `[OITC]` + Arena + Status + Spielerzahl
- **Scoreboard:** Live-Kills, Restzeit, Ziel
- **Bossbar:** Countdown + Fortschrittsanzeige

---

## Punkte-Integration (optional)

Mit aktivem **RankPoints-Hook**:
- Punkte für Kills & Siege
- `commitSessionPoints()` am Ende
- sicherer Fallback, falls RankPoints deaktiviert

---

## Daten & Persistenz

- Sessions & Schilder werden persistent gespeichert.
- Alte Sessions werden automatisch entfernt.
- `/oitc reset` leert alles und teleportiert alle Spieler zurück.

---

## Entwickler-Notizen (Architektur)

- Modularer Aufbau (`managers/`, `listeners/`, `commands/`)
- **GameManager** als zentrales Bindeglied
- **SpectatorManager** isoliert Zuschauer
- **CombatManager** regelt Treffer, Void, Pfeile
- **MatchManager** Start/Stop, Bossbar, Siegauswertung

---

## FAQ

**Countdown startet nicht:** prüfe `minPlayers` und dass keine Zuschauer mitgezählt werden.  
**Void-Tod ohne Pfeil:** Du bekommst nur Bogen + Schwert, keinen neuen Pfeil.  
**Spiel stoppt nicht:** Zuschauer zählen nicht für Abbruch.  
**Join während Spiel:** wird automatisch Spectator.

---

## Lizenz

© 2025 Kantonsschule Reussbühl / KSR Minecraft.  
Freigegeben unter der [MIT-Lizenz](https://opensource.org/licenses/MIT).
