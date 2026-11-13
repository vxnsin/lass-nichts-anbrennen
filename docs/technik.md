# Technik

Der Server ist ein eigenständiges Programm auf Basis von [Minestom](https://minestom.net/) für Minecraft 26.2
und Java 25. Minestom bringt kein Vanilla-Verhalten mit: Es gibt keine Mobs, kein Wetter und kein Feuer, das
sich von allein ausbreitet. Alles, was im Spiel passiert, steht in diesem Repo.

## Vom Inhalt zum Spiel

```
content/  ──►  packbuilder  ──►  pack.zip, registry.json, credits.json  ──►  LassNichtsAnbrennen.jar
```

Der Pack-Builder läuft beim Bauen. Er prüft `content/` (fehlende Texturen, doppelte IDs, fehlende Einträge in
`CREDITS.md`), schreibt Item-Definitionen, `sounds.json` und die HUD-Schrift und packt alles reproduzierbar
zusammen. Der Hash des Packs ändert sich also nur, wenn sich der Inhalt ändert. Maps, Missionen und Sprachdateien
wandern unverändert in die Jar. Liegt neben dem Server ein Ordner `content/`, haben Dateien darin Vorrang.

Beim Verbinden schickt der Server Adresse und Hash des Packs an den Client. Ausgeliefert wird es von einem
kleinen HTTP-Server im selben Programm (Port 8164) oder von einer festen Adresse aus der `server.yml`.

## Die Welt

Die Welt existiert nur im Arbeitsspeicher. Bei jedem Start liest der Server die Grundrisse aus `content/maps/`
und baut sie nebeneinander auf, der Hub zuerst. Es gibt keinen Weltordner und nichts einzurichten.

Eine Map ist eine YAML-Datei mit einem Textraster: Jedes Zeichen steht für eine Blocksäule, die Legende erklärt
die Zeichen. Gefahrenstellen, Infotexte und Missionspunkte stehen in derselben Datei. Das Format ist im Kopf von
[`content/maps/hub.yml`](../content/maps/hub.yml) beschrieben. Ein Test prüft bei jedem Build, dass
Zeilenbreiten, Legende und Verweise auf Items und Missionen stimmen.

## Pakete im Server

| Paket | Aufgabe |
|---|---|
| `map`, `world` | Grundrisse lesen, Welt bauen, Türen |
| `item` | Eigene Items aus der Registry. Erkannt werden sie über ein Datenfeld am Item, nicht über den Namen |
| `furniture` | Möbel, Schilder und Deko als Item-Display mit einer Interaction-Entity als Klickfläche, bei Bedarf mit Barrier-Blöcken als Kollision |
| `fire` | Feuer mit Brandklassen und Löschmitteln |
| `extinguisher` | Sprühen, Füllstand und Wirkung je Brandklasse |
| `hud` | Bildzeichen in Texten und eine HUD-Zeile über den Titel einer unsichtbaren Bossbar |
| `mission` | Ablauf der Missionen, Gruppen, Auswertung, Brandschutz-Pass |
| `lobby` | Missionstisch und Figuren im Hub |
| `lang` | Texte auf Deutsch und Englisch, je nach Sprache des Minecraft-Clients |
| `command` | Befehle für Spieler, Operatoren und die Konsole |
| `console` | Startblock, Logzeilen und Befehlseingabe |
| `pack` | Auslieferung des Resource Packs |

## Dateien neben dem Server

| Datei | Inhalt |
|---|---|
| `server.yml` | Ports, Kontoprüfung, Text in der Serverliste, Pack-Adresse. Wird beim ersten Start angelegt |
| `ops.txt` | Operatoren, ein Name pro Zeile |
| `passes.yml` | Stempel im Brandschutz-Pass je Spieler |
