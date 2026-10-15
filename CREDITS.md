# Quellen und Lizenzen

Jede Datei in `content/textures`, `content/models`, `content/sounds` und `content/entities` braucht hier
einen Eintrag, auch eigene Werke. Der Pack-Builder bricht sonst ab.

Regeln für fremde Dateien (siehe [docs/PLAN.md](docs/PLAN.md), Abschnitt 5):

- **Erlaubt:** CC0, CC BY, CC BY-SA
- **Vermeiden:** CC BY-NC (der Builder warnt)
- **Verboten:** CC BY-ND und Dateien ohne klare Lizenz (der Builder bricht ab)
- **Musik:** nur eigene Stücke oder Stücke von der JWSL-Website (Vorgabe des Musikformulars)

Die Liste ist im Spiel mit `/credits` abrufbar und gehört in die Beitragsbeschreibung
(„Genutzte Quellen und Medien“).

## Eigene und fremde Assets (von Hand gepflegt)

Hier alles eintragen, was nicht vom Generator kommt: selbst gezeichnete Texturen, eigene
Blockbench-Modelle, Sounds und Dateien aus dem Internet.

| Datei | Titel | Urheber*in | Quelle | Lizenz | Änderungen |
|---|---|---|---|---|---|

## Generierte Assets

Diese Tabelle schreibt `uv run tools/generate_assets.py` automatisch neu. Bitte nicht von Hand ändern.

<!-- assetgen:start -->
| Datei | Titel | Urheber*in | Quelle | Lizenz | Änderungen |
|---|---|---|---|---|---|
| models/furniture/akku_ladeschrank.json | Akku-Ladeschrank (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/akku_quarantaenebox.json | Akku-Quarantänebox (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/brandschutztuer.json | Brandschutztür (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/druckknopfmelder.json | Druckknopfmelder (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/feuerloescher_halter.json | Feuerlöscher-Wandhalter (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/fritteuse.json | Fritteuse (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/heizluefter.json | Heizlüfter (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/herd.json | Herd mit Topf (Öl) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/kaffeemaschine.json | Kaffeemaschine (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/rauchmelder.json | Rauchmelder (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/sprinkler.json | Sprinklerkopf (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/furniture/wandhydrant.json | Wandhydrant (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/akku.json | Akku (intakt) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/akku_beschaedigt.json | Akku (beschädigt) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/akku_geblaeht.json | Akku (aufgebläht) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/brandschutzhelfer_ausweis.json | Brandschutzhelfer-Ausweis (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_abc.json | ABC-Pulverlöscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_abc_aim.json | ABC-Pulverlöscher (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_abc_draw.json | ABC-Pulverlöscher (3D-Modell, Schlauch ziehen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_co2.json | CO2-Löscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_co2_aim.json | CO2-Löscher (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_co2_draw.json | CO2-Löscher (3D-Modell, Schlauch ziehen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_fett.json | Fettbrandlöscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_fett_aim.json | Fettbrandlöscher (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_fett_draw.json | Fettbrandlöscher (3D-Modell, Schlauch ziehen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_metall.json | Metallbrandlöscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_metall_aim.json | Metallbrandlöscher (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_metall_draw.json | Metallbrandlöscher (3D-Modell, Schlauch ziehen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_schaum.json | Schaumlöscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_schaum_aim.json | Schaumlöscher (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_schaum_draw.json | Schaumlöscher (3D-Modell, Schlauch ziehen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_wasser.json | Wasserlöscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_wasser_aim.json | Wasserlöscher (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloescher_wasser_draw.json | Wasserlöscher (3D-Modell, Schlauch ziehen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloeschspray.json | Feuerlöschspray (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/feuerloeschspray_aim.json | Feuerlöschspray (3D-Modell, zielen) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/handy.json | Handy (Notruf 112) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/holzkeil.json | Holzkeil (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/kabel_beschaedigt.json | Kabel (beschädigte Isolierung) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/klemmbrett.json | Klemmbrett mit Checkliste (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/ladegeraet.json | Ladegerät (Original) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/ladegeraet_billig.json | Ladegerät (Billig, beschädigt) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/loeschdecke.json | Löschdecke (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/mehrfachsteckdose.json | Mehrfachsteckdose (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/powerbank.json | Powerbank (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/powerbank_geblaeht.json | Powerbank (aufgebläht) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/topfdeckel.json | Topfdeckel (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/item/warnweste.json | Warnweste (gefaltet) (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/fluchtplan.json | Flucht- und Rettungsplan (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/schild_brandmelder.json | Schild Brandmelder (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/schild_erste_hilfe.json | Schild Erste Hilfe (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/schild_feuerloescher.json | Schild Feuerlöscher (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/schild_loeschschlauch.json | Schild Löschschlauch (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/schild_notausgang.json | Schild Notausgang (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| models/sign/schild_sammelstelle.json | Schild Sammelstelle (3D-Modell) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| sounds/feuerloescher/spray.ogg | Feuerlöscher sprüht (synthetisch) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/akku_ladeschrank.png | Akku-Ladeschrank (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/akku_quarantaenebox.png | Akku-Quarantänebox (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/brandschutztuer.png | Brandschutztür (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/druckknopfmelder.png | Druckknopfmelder (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/feuerloescher_halter.png | Feuerlöscher-Wandhalter (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/fritteuse.png | Fritteuse (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/heizluefter.png | Heizlüfter (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/herd.png | Herd mit Topf (Öl) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/kaffeemaschine.png | Kaffeemaschine (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/rauchmelder.png | Rauchmelder (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/sprinkler.png | Sprinklerkopf (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/furniture/wandhydrant.png | Wandhydrant (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/hud/bar_empty.png | Füllstandsbalken leer (HUD) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/hud/bar_full.png | Füllstandsbalken voll (HUD) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/hud/feuerloescher.png | Feuerlöscher-Symbol (HUD) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/akku.png | Akku (intakt) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/akku_beschaedigt.png | Akku (beschädigt) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/akku_geblaeht.png | Akku (aufgebläht) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/brandschutzhelfer_ausweis.png | Brandschutzhelfer-Ausweis (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloescher_abc.png | ABC-Pulverlöscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloescher_co2.png | CO2-Löscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloescher_fett.png | Fettbrandlöscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloescher_metall.png | Metallbrandlöscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloescher_schaum.png | Schaumlöscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloescher_wasser.png | Wasserlöscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/feuerloeschspray.png | Feuerlöschspray (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/handy.png | Handy (Notruf 112) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/holzkeil.png | Holzkeil (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/kabel_beschaedigt.png | Kabel (beschädigte Isolierung) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/klemmbrett.png | Klemmbrett mit Checkliste (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/ladegeraet.png | Ladegerät (Original) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/ladegeraet_billig.png | Ladegerät (Billig, beschädigt) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/loeschdecke.png | Löschdecke (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/mehrfachsteckdose.png | Mehrfachsteckdose (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/powerbank.png | Powerbank (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/powerbank_geblaeht.png | Powerbank (aufgebläht) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/topfdeckel.png | Topfdeckel (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/item/warnweste.png | Warnweste (gefaltet) (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/fluchtplan.png | Flucht- und Rettungsplan (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/schild_brandmelder.png | Schild Brandmelder (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/schild_erste_hilfe.png | Schild Erste Hilfe (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/schild_feuerloescher.png | Schild Feuerlöscher (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/schild_loeschschlauch.png | Schild Löschschlauch (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/schild_notausgang.png | Schild Notausgang (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
| textures/sign/schild_sammelstelle.png | Schild Sammelstelle (Textur) | vxnsin, mit KI-Unterstützung erzeugt | tools/generate_assets.py | CC BY 4.0 | Platzhalter |
<!-- assetgen:end -->
