# Konzept

Ein Minecraft-Spielmodus (Einzel- und Mehrspieler), in dem man als neue*r Azubi beim fiktiven Online-Händler **„BrandNeu Versand GmbH“** in mehreren Missionen Brandgefahren erkennt, Brände richtig bekämpft und im Ernstfall alle Kolleg*innen sicher zum Sammelplatz bringt. Alle Texturen, Modelle und Texte machen wir selbst, und der Quellcode erscheint offen auf GitHub.

## Lernziele

Jede Mission deckt Module des offiziellen Unterrichtskonzepts ab. So lässt sich in der Beitragsbeschreibung zeigen, dass wir das Thema verstanden haben.

| JWSL-Modul | Mission im Spiel |
|---|---|
| 1 Warum Brandschutz wichtig ist | Hub/Tutorial |
| 2 Wie entsteht ein Brand? Branddreieck | M1 Branddreieck-Labor |
| 3 Brandschutz in Betrieb und Berufsschule | Hub + M2 |
| 4 Baulicher und technischer Brandschutz | M2 Büro (Brandschutztür, Rauchmelder, Fluchtwege) |
| 5 Organisatorischer Brandschutz und Rollen | M5 Finale (Brandschutz-/Evakuierungshelfer*in) |
| 6 Verhalten im Brandfall | M5 Finale |
| 7 Erste Hilfe im Brandfall | M5 Finale (Ersthelfer*in-Rolle) |
| 8 Wie kann ein Brand gelöscht werden? | M4 Pausenküche & Feuerlöscher-Training |
| 9 Elektrische Gefahren & Lithium-Ionen-Akkus | M3 Retourenlager |

## Kampagne

**Rahmenhandlung:** Erster Ausbildungstag bei der BrandNeu Versand GmbH. Frau Funke, die Brandschutzbeauftragte, nimmt dich unter ihre Fittiche. Mit dabei ist das Maskottchen **„Löschi“**, ein kleiner Feuerlöscher mit Gesicht, der Tipps gibt. Nach jeder Mission gibt es einen Stempel im **Brandschutz-Pass**. Mit vollem Pass startet das Finale.

Grundprinzip: **Fehler werden erklärt, nicht bestraft.** Wer falsch handelt, sieht die Folge (z. B. Stichflamme) und bekommt danach kurz erklärt, warum.

### Hub – Berufsschule & Firmengelände (Tutorial)
- Spawn im Klassenzimmer, Missions-Tafel mit Brandschutz-Pass
- Tutorial: Flucht- und Rettungsplan lesen, Sammelplatz finden, grüne Rettungszeichen von roten Brandschutzzeichen unterscheiden
- Infotafeln: „Warum Brandschutz?“, Brandschutzordnung Teil A/B/C kurz erklärt
- Portale/NPCs zu den Missionen

### M1 – Das Branddreieck (Labor/Werkstatt)
- Puzzle: Drei Brände, jedes Mal muss eine Seite des Branddreiecks entfernt werden (Brennstoff, Sauerstoff, Zündquelle/Wärme)
- Beispiele: Deckel auf Metallbehälter (Sauerstoff), Gasventil zu (Brennstoff), Lötkolben aus und weglegen (Zündquelle)
- Abschluss: Mini-Quiz mit 3 Fragen

### M2 – Gefahren-Detektiv im Büro (Suchspiel)
- Zeitlimit (z. B. 5 Min.), 10 versteckte Gefahren finden und per Rechtsklick „melden“:
  - Mehrfachsteckdosen hintereinander gesteckt
  - Kabel mit beschädigter Isolierung
  - Brandschutztür mit Holzkeil offengehalten
  - Fluchtweg mit Kartons zugestellt
  - Feuerlöscher hinter Regal verdeckt
  - Papierstapel direkt am Heizlüfter
  - Kaffeemaschine/Wasserkocher läuft unbeaufsichtigt
  - Aufgeblähte Powerbank lädt auf dem Schreibtisch
  - Rauchmelder abgeklebt
  - Zigarettenreste im Papierkorb
- Bewertung: gefundene Gefahren, Zeit, falsch gemeldete Dinge

### M3 – Akku-Alarm im Retourenlager (E-Commerce-Bezug)
- Retouren-Annahme: Pakete öffnen und Akkus prüfen (intakt / beschädigt / gebläht). Beschädigte kommen in die **Akku-Quarantänebox**, nicht ins normale Regal
- Ladezone für Handscanner/Flurförderzeuge: falsche Ladegeräte, Laden auf Karton, ungesicherte Ladekabel finden
- Event: Ein übersehener Akku gerät in **thermisches Durchgehen**. Richtig: Abstand, Alarm auslösen, Feuerwehr rufen, Raum verlassen, Tür schließen. Nur bei sehr kleinem Gerät ein Löschversuch nach Anweisung
- Fakt im Spiel: Elektrizität ist seit Jahren für rund 30 % der vom IFS untersuchten Brände verantwortlich, Akkus mit wachsendem Anteil

### M4 – Fettbrand in der Pausenküche + Feuerlöscher-Training
- Teil A: Topf mit Öl auf dem Herd fängt Feuer.
  - Falsch (Wasser) → Fettexplosion als Effekt, danach Erklärung
  - Richtig: Herd aus, Deckel drauf oder Löschdecke, Fettbrandlöscher (Brandklasse F)
- Teil B: **Löscher-Wand**. Zu jedem Brand den passenden Löscher wählen:
  - A: Papier/Holz
  - B: Lösungsmittel
  - C: Gas
  - D: Metall
  - F: Fett
  - Elektrogerät (keine eigene Brandklasse, Löscher mit Spannungsangabe, z. B. CO₂)
- Teil C: **Handhabung**. Sicherung ziehen, mit Windrichtung, Flächenbrand von vorn nach hinten, Tropfbrand von oben nach unten, Wandbrand von unten nach oben. Stoßweise löschen, mehrere Löscher gleichzeitig statt nacheinander, auf Wiederentzündung achten. Benutzte Löscher nicht zurückhängen, sondern melden.

### M5 – Ernstfall im Logistikzentrum (Multiplayer-Finale, 2–6 Spieler*innen)
- Großbrand mit Rauchausbreitung. Rauch nimmt Sicht und verursacht Schaden, geduckt laufen hilft.
- Rollen (bei Einzelspieler übernehmen NPCs):
  - **Brandschutzhelfer*in**: Löschversuch nur ohne Eigengefährdung
  - **Evakuierungshelfer*in**: Räume kontrollieren, Türen schließen, Personen mit eingeschränkter Mobilität helfen
  - **Ersthelfer*in**: Person mit brennender Kleidung → Löschdecke/am Boden wälzen, Notruf, Verbrennung versorgen
  - **Notruf**: Dialog mit den W-Fragen (Wo? Was? Wie viele? Wer meldet? Auf Rückfragen warten)
- Pflichtschritte: Ruhe bewahren → Druckknopfmelder → Notruf 112 → andere warnen → **kein Aufzug** → grünen Fluchtwegschildern folgen → Sammelplatz → Vollzähligkeit melden
- Ende: Feuerwehr-NPC kommt an, Auswertung pro Team, Brandschutz-Pass wird zum **Abschlusszertifikat**

### Wenn Zeit bleibt
- **Quiz-Arena** (Multiplayer, Klasse gegen Klasse) mit Fragen aus einer YAML-Datei
- Highscore-Tafel im Hub
- Englische Übersetzung

## Faktencheck

Jede Infotafel und jede Quizfrage bekommt eine Quelle. Vor dem Release prüft die Lehrkraft oder die Brandschutzbeauftragte des Ausbildungsbetriebs die Texte.

- JWSL-Unterrichtskonzept: 9 Module mit Wissensseiten und Arbeitsblättern. **Wichtigste Quelle**, Texte daran ausrichten (v. a. Wissensseiten 6, 7, 8, 9)
- ASR A2.2 „Maßnahmen gegen Brände“: Brandklassen, Löscher-Kennzeichnung, Brandschutzhelfer (in der Regel 5 % der Beschäftigten)
- DIN EN 2 Brandklassen: A fest, B flüssig, C Gase, D Metalle, F Speisefette; Elektro ohne eigene Klasse
- Brandschutzordnung Teil A (DIN 14096): Ruhe bewahren, Brand melden, in Sicherheit bringen, Löschversuch unternehmen
- IFS/GDV: Elektrizität als häufigste Brandursache (~30 %), Akku-Anteil steigt
- **Noch abgleichen** mit JWSL-Wissensseite 7: genaue Erste-Hilfe-Schritte bei Verbrennungen (Kühlen ja/wie lange)

## Quellen

- BGHW eMagazin: <https://hundertprozent.bghw.de/azubis-lass-nichts-anbrennen>
- JWSL: <https://www.jwsl.de/>, Unterrichtskonzept: <https://www.jwsl.de/lehrkraefte/unterrichtskonzept>
- DGUV Pressemitteilung: <https://dguv.de/de/mediencenter/pm/jwsl-2026-27.jsp>
- ASR A2.2 (Volltext): <https://umwelt-online.de/recht/arbeitss/arbst.vo/arbst.rl/a2_2.htm>
- Elektrizität als Brandursache: <https://www.hessenschau.de/panorama/elektrizitaet-ist-brandursache-nummer-eins-v1,kurz-technik-brandursache-100.html>
- Akkubrände: <https://www.dieversicherer.de/versicherer/wohnen/news/akkubraende-143978>
- Brandschutzordnung Teil A, Muster für Schulen: <https://nibis.de/uploads/redmey/Musterkonzepte/Arbeitsschutz_Gesundheitsmanagem/Muster-BSO-Schulen_10-2018.pdf>
