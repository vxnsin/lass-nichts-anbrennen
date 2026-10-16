# Lass nichts anbrennen – Minecraft-Kampagne zum Brandschutz

Ein Minecraft-Spielmodus (Paper-Server, Einzel- und Mehrspieler), in dem Azubis Brandgefahren erkennen,
Brände richtig bekämpfen und im Ernstfall alle sicher zum Sammelplatz bringen. Entstanden als Beitrag
der Klasse WEC10A (E-Commerce-Kaufleute) zum Kreativwettbewerb **„Jugend will sich-er-leben“ 2026/27**
mit dem Jahresthema *Brandschutz in Ausbildung und Beruf*.

Projektplan, Missionen und Quellen: [docs/PLAN.md](docs/PLAN.md)

## Stand

| Baustein | Status |
|---|---|
| Eigener Resource-Pack-Builder (Items, Sounds, Glyphen, Benutzt-Modelle, CREDITS-Prüfung) | ✅ |
| Pack-Auslieferung über eingebauten HTTP-Server | ✅ |
| Glyphen-/HUD-System (`<glyph:…>`, `<shift:…>`, Bossbar-HUD) | ✅ |
| Asset-Generator: 42 3D-Modelle (Löscher, Ausrüstung, Elektro, Gebäude, Küche, Schilder) | ✅ Platzhalter |
| Feuerlöscher: Schlauch-Animation beim Benutzen, Sprühen ohne Verlangsamung, Füllstands-HUD | ✅ |
| Möbel, Schilder und Deko platzieren (Display-Entities mit Hitbox) | ✅ |
| Feuer-Simulation mit Brandklassen (Fettexplosion, Stromschlag, Wiederentzündung) | ✅ |
| Map-Generator (ASCII-Grundrisse): Hub und Büro | ✅ |
| Lobby: Kampagnen-Menü, Missionstisch, Gruppen, vxnsin-Ecke | ✅ |
| Mission 2 „Gefahren-Detektiv“ | ✅ |
| Missionen 1, 3, 4, 5 · NPCs mit Animation (BetterModel) | geplant |

Alles ab „Möbel“ ist bisher nur gebaut und per Test geprüft, **im Spiel noch nicht ausprobiert**.

## Voraussetzungen

- **JDK 25** (z. B. [Eclipse Temurin](https://adoptium.net/)). Gradle lädt sich selbst über den Wrapper.
- Zum Spielen: **Minecraft Java Edition 26.3**

## Bauen

```bash
./gradlew build
```

Ergebnis:

- `plugin/build/libs/LassNichtsAnbrennen-<version>.jar`: Plugin, enthält das Resource Pack
- `packbuilder/build/pack/pack.zip`: das Resource Pack einzeln

Unter Windows `gradlew.bat` statt `./gradlew` verwenden.

## Dev-Server starten

```bash
./gradlew dev
```

Startet Paper 26.3 in `plugin/run/` mit diesem Plugin, **WorldEdit** und **BetterModel**. Alles wird automatisch
heruntergeladen. Beim ersten Start werden Entwickler-Einstellungen geschrieben (Kreativmodus, flache Welt,
friedlich, keine Whitelist).

**Minecraft-EULA:** Bitte die [EULA](https://aka.ms/MinecraftEULA) lesen. Wer zustimmt, trägt einmalig
`lna.acceptMinecraftEula=true` in `~/.gradle/gradle.properties` ein. Ohne den Eintrag stoppt der Server beim
ersten Start und man setzt in `plugin/run/eula.txt` selbst `eula=true`.

Erster Test im Spiel (Server-Konsole: `op <Name>`):

1. `/lna map build hub` → baut den Hub an deiner Position, danach `/lna map build buero` ein Stück daneben
2. Rejoinen oder Menü-Item (Slot 5) benutzen → Kampagnen-Menü
3. Am Missionstisch die Karte „Mission 2“ anklicken → **[Mission starten]**

| Befehl | Wirkung |
|---|---|
| `/mission` · `/mission start <id>` · `/mission stop` | Missionen anzeigen, starten (für die ganze Gruppe), abbrechen |
| `/gruppe` · `/gruppe einladen <Name>` · `/gruppe annehmen` · `/gruppe verlassen` | Gruppen für Mehrspieler |
| `/lna map list` · `/lna map build <map>` · `/lna map tp <map>` | Maps aus `plugin/src/main/resources/maps/` bauen |
| `/lna fire start <a\|b\|c\|d\|f\|elektro> [stärke]` · `/lna fire list` · `/lna fire clear` | Testbrand am angeschauten Block |
| `/lna give <item>` · `/lna items` · `/lna refill` | Eigene Items, Feuerlöscher auffüllen |
| `/lna pack` · `/lna reloadpack` | Resource Pack neu laden bzw. neu gebautes Pack an alle senden |
| `/credits` | Alle Quellen und Lizenzen |

Bauen im Spiel (Berechtigung `lna.build`): Möbel oder Schild in die Hand und **Rechtsklick** auf Boden, Wand oder
Decke. Andere eigene Items stellt man mit **Shift + Rechtsklick** als Deko ab. **Shift + Linksklick** entfernt
ein Objekt. Stehen Objekte verkehrt herum, in der Config `furniture.rotation-offset: 180` setzen.

Schneller Testkreislauf für Texturen und Sounds, ohne Server-Neustart:

```bash
./gradlew :plugin:devPack
```

danach im Spiel `/lna reloadpack`.

Auf einem eigenen Server: Jar nach `plugins/` kopieren. In `plugins/LassNichtsAnbrennen/config.yml` unter
`pack.host` die öffentliche Adresse eintragen und Port `8164` freigeben.

## Inhalte hinzufügen

Alle Inhalte liegen in [`content/`](content) und werden beim Bauen geprüft. Fehler meldet der Builder
mit Datei und Grund.

1. Textur nach `content/textures/item/`, Sound als **Mono-.ogg** nach `content/sounds/`
2. Eintrag in [`content/items.yml`](content/items.yml), [`content/sounds.yml`](content/sounds.yml) oder
   [`content/glyphs.yml`](content/glyphs.yml). Das Format ist oben in jeder Datei erklärt
3. **Jede Datei in [`CREDITS.md`](CREDITS.md) eintragen**, auch eigene. Sonst bricht der Build ab
4. `./gradlew build`

Glyphen funktionieren in jedem Text, den das Plugin verschickt:
`<glyph:hud_feuerloescher><shift:2>Text` setzt ein Bild und rückt 2 Pixel weiter.

### Generierte Modelle

Die Platzhalter-Modelle, Texturen, HUD-Grafiken und der Sprüh-Sound entstehen per Skript
(Python-Paket [`tools/assetgen/`](tools/assetgen), ein Modul pro Themengruppe):

```bash
uv run tools/generate_assets.py
```

Das Skript schreibt nach `content/`, pflegt die Tabelle „Generierte Assets“ in `CREDITS.md` und löscht
Dateien, die es nicht mehr erzeugt. Die Modelle sind normale Java-Item-Modelle und lassen sich in
**Blockbench** öffnen und verfeinern. Wer ein Modell von Hand überarbeitet, nimmt es aus dem Generator heraus
und trägt es in `CREDITS.md` unter „von Hand gepflegt“ ein.

Beim Benutzen (Rechtsklick halten) wechselt der Client selbst zwischen Modellen. Das wird in `items.yml`
über `use_models` gesteuert, wie beim Bogen.

## Projektstruktur

```
├─ packbuilder/   Resource-Pack-Builder (Java, läuft beim Bauen)
├─ plugin/        Paper-Plugin (Java 25)
├─ content/       Items, Sounds, Glyphen, Texturen, Modelle
├─ tools/         Asset-Generator (Python, uv)
├─ docs/          Projektplan
├─ CREDITS.md     Quellen und Lizenzen aller Assets
└─ gradle/        Wrapper und Versionskatalog (libs.versions.toml)
```

## Hinweis zur KI-Nutzung

Planung, Grundgerüst, Asset-Generator und alle Platzhalter-Assets (3D-Modelle, Texturen, HUD-Grafiken,
Sprüh-Sound) sind mit Unterstützung von Claude (Anthropic) entstanden. Das ist in [`CREDITS.md`](CREDITS.md) und in der
Beitragsbeschreibung angegeben. Idee, Konzept, Projektstruktur und Review stammen vom Projektteam.

## Lizenz

- Code: [MIT](LICENSE)
- Eigene Assets (Texturen, Modelle, Sounds, Texte): [CC BY 4.0](LICENSE-ASSETS)
- Fremde Assets: jeweilige Lizenz laut [`CREDITS.md`](CREDITS.md)

Minecraft ist eine Marke von Mojang Studios. Dieses Projekt steht in keiner Verbindung zu Mojang,
Microsoft, der DGUV oder der BGHW.
