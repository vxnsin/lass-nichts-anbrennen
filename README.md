<p align="center">
  <img src="docs/assets/wordmark.svg" alt="Lass nichts anbrennen" width="420">
</p>

<p align="center">
  Eine Minecraft-Kampagne, in der man Brandschutz nicht liest, sondern spielt.
</p>

---

Du fängst als Azubi bei der erfundenen BrandNeu Versand GmbH an und lernst in Missionen, wie Brände entstehen,
wo im Büro die Gefahren stecken und welcher Löscher zu welchem Brand gehört. Wer falsch handelt, sieht die
Folge und bekommt erklärt, warum. Bestraft wird niemand.

Das Projekt ist der Beitrag der Klasse WEC10A (Kaufleute im E-Commerce) zum Kreativwettbewerb
„Jugend will sich-er-leben“ 2026/27 mit dem Jahresthema *Brandschutz in Ausbildung und Beruf*.

## Missionen

| Mission | Ort | Worum es geht |
|---|---|---|
| Das Branddreieck | Labor | Drei Brände löschen, indem man jeweils eine Seite des Branddreiecks entfernt, danach ein Quiz |
| Gefahren-Detektiv | Büro | Zehn versteckte Brandgefahren finden, bevor die Zeit abläuft |
| Fettbrand und Löscher-Training | Küche | Einen Fettbrand richtig löschen und für jeden Brand den passenden Löscher wählen |

Jede bestandene Mission bringt einen Stempel im Brandschutz-Pass. Die Geschichte und die Lernziele stehen in
[docs/konzept.md](docs/konzept.md).

## Loslegen

Du brauchst ein **JDK 25** (zum Beispiel [Temurin](https://adoptium.net/)) und zum Spielen
**Minecraft Java Edition 26.2**.

```bash
git clone https://github.com/vxnsin/lass-nichts-anbrennen
cd lass-nichts-anbrennen
./gradlew dev
```

Unter Windows heißt der Befehl `gradlew.bat dev`. Er baut den Server samt Resource Pack und startet ihn in
`server/run/`. Die Welt mit allen Maps entsteht bei jedem Start von selbst, einzurichten gibt es nichts.
Danach in Minecraft mit `localhost` verbinden: Du stehst im Hub vor dem Missionstisch.

### Als fertige Datei

```bash
./gradlew :server:jar
```

Das Ergebnis ist `server/build/libs/LassNichtsAnbrennen-<version>.jar`, eine einzelne Datei ohne weitere
Abhängigkeiten:

```bash
java -jar LassNichtsAnbrennen-<version>.jar
```

Beim ersten Start legt der Server eine kommentierte `server.yml` neben sich. Damit andere mitspielen können,
müssen die Ports `25565` (Spiel) und `8164` (Resource Pack) erreichbar sein. Ist der Server aus dem Internet
erreichbar, gehört unter `pack.host` seine öffentliche Adresse.

## Befehle

Für alle Spieler:

| Befehl | Wirkung |
|---|---|
| `/mission`, `/mission start <id>`, `/mission stop` | Missionen anzeigen, für die Gruppe starten, abbrechen |
| `/gruppe einladen <Name>`, `/gruppe annehmen`, `/gruppe verlassen` | Missionen gemeinsam spielen, auf Englisch `/party invite`, `accept`, `leave` |
| `/credits` | Quellen und Lizenzen aller Assets |

In der Server-Konsole, mit oder ohne Schrägstrich:

| Befehl | Wirkung |
|---|---|
| `help` | Alle Befehle |
| `list` | Wer online ist |
| `op <Name>`, `deop <Name>` | Operator-Rechte geben und nehmen |
| `stop` | Server beenden |

Operatoren dürfen die Konsolenbefehle auch im Spiel benutzen und haben dort zusätzlich Werkzeuge zum
Ausprobieren:

| Befehl | Wirkung |
|---|---|
| `/give <item>` | Ein eigenes Item holen |
| `/refill` | Den Löscher in der Hand auffüllen |
| `/fire start <a\|b\|c\|d\|f\|electric> [stärke]`, `/fire clear` | Testbrand auf dem angeschauten Block, alle Brände entfernen |

## Sprache

Was Spieler lesen, richtet sich nach der Sprache ihres Minecraft-Clients: Deutsch oder Englisch. Die Texte
stehen in `content/lang/de.yml` und `content/lang/en.yml`. Infotafeln, Gefahrenbeschreibungen und Quizfragen
aus den Map- und Missionsdateien sind deutsch.

## Inhalte ändern

Alles, was man ohne Java anfassen kann, liegt in [`content/`](content):

| Datei oder Ordner | Inhalt |
|---|---|
| `items.yml` | Items, Möbel und Schilder mit Namen, Beschreibung und Verhalten |
| `missions.yml` | Missionen, Zeitlimits und Quizfragen |
| `maps/` | Grundrisse der Maps als Textraster, dazu Infotexte und Gefahrenstellen |
| `lang/` | Meldungen auf Deutsch und Englisch |
| `sounds.yml`, `glyphs.yml` | Sounds und Bildzeichen für das HUD |
| `textures/`, `models/`, `sounds/` | Die Dateien dazu |

Jede Textur, jedes Modell und jeder Sound braucht eine Zeile in [`CREDITS.md`](CREDITS.md), auch eigene
Werke. Fehlt sie, bricht der Build mit einer Meldung ab, die Datei und Grund nennt.

Modelle und Texturen erzeugt ein Skript:

```bash
uv run tools/generate_assets.py
```

Es schreibt nach `content/` und pflegt seinen Teil von `CREDITS.md` selbst. Die Modelle sind gewöhnliche
Java-Item-Modelle und lassen sich in Blockbench öffnen. Wer eines von Hand überarbeitet, nimmt es aus dem
Generator heraus.

Liegt neben einer laufenden Jar ein Ordner `content/` mit Maps, Missionen oder Sprachdateien, haben diese
Dateien Vorrang vor den eingebauten. So lassen sich Texte und Grundrisse ändern, ohne neu zu bauen.

## Aufbau

```
content/       Items, Missionen, Maps, Texte, Texturen, Modelle, Sounds
server/        der Server mit der Spiellogik
packbuilder/   baut aus content/ das Resource Pack und prüft die Credits
tools/         Generator für Modelle und Texturen (Python)
docs/          Konzept, Technik, Wettbewerbsregeln
```

Wie die Teile zusammenspielen, beschreibt [docs/technik.md](docs/technik.md).

## Wer dahinter steckt

Idee, Konzept und Code stammen von [vxnsin](https://github.com/vxnsin), im Spiel als „vensin“ unterwegs
und selbst in der Ausbildung im E-Commerce. Im Hub steht dazu eine eigene Ecke.

Danke an

- die Klasse WEC10A, für die das Projekt als Wettbewerbsbeitrag entsteht,
- das Programm [Jugend will sich-er-leben](https://www.jwsl.de/) der DGUV, dessen Unterrichtskonzept die
  fachliche Grundlage aller Spieltexte ist,
- [Minestom](https://minestom.net/), auf dem der Server aufbaut, und
  [Adventure](https://docs.advntr.dev/) für Texte und Farben.

Die vollständige Liste aller Assets mit Urheber und Lizenz steht in [CREDITS.md](CREDITS.md).

## KI-Nutzung

Der Wettbewerb verlangt, den Einsatz von KI offenzulegen. Bei Planung, Code, dem Asset-Generator und den
damit erzeugten Modellen, Texturen und Sounds hat Claude (Anthropic) geholfen. Idee, Konzept, Entscheidungen
und Prüfung liegen beim Projektteam.

## Lizenz

Der Code steht unter [MIT](LICENSE), eigene Texturen, Modelle, Sounds und Texte unter
[CC BY 4.0](LICENSE-ASSETS). Für fremde Assets gilt die jeweilige Lizenz laut [CREDITS.md](CREDITS.md).

Minecraft ist eine Marke von Mojang Studios. Dieses Projekt steht in keiner Verbindung zu Mojang, Microsoft,
der DGUV oder der BGHW.
