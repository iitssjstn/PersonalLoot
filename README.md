# PersonalLoot

Paper/Purpur plugin voor **Minecraft 26.1.2**: elke speler krijgt zijn **eigen loot** uit kisten, barrels en minecarts met kist die de wereld zelf heeft gemaakt (dungeons, mineshafts, pillager outposts, tempels, trial chambers, enz.).

Kisten die jij nog niet geopend hebt krijgen een **gouden rand + gouden slotje**, alleen zichtbaar voor jou. Zodra je de kist opent verdwijnt de rand voor jou; andere spelers zien hem nog.

## Features
- Per-speler loot voor kisten, trapped chests, barrels en minecarts met kist
- Werkt automatisch in alle structuren en nieuwe chunks
- Deksel-animatie en geluid werken normaal
- Gouden rand (of gekleurd laagje) over kisten die je nog niet geopend hebt, zonder resource pack
- Bescherming tegen breken, hoppers en explosies
- `/ploot set <loottable>` om oude, al geopende kisten weer persoonlijk te maken
- Alle data wordt in de kist zelf opgeslagen, geen database nodig

## Installeren
1. Download de nieuwste `.jar` bij [**Releases**](https://github.com/iitssjstn/PersonalLoot/releases/latest).
2. Zet hem in `plugins/` en herstart de server.
3. Gebruik niet tegelijk met andere per-speler-loot plugins (Lootr, Lootin, JustLootIt).

## Commando's (`/ploot`, alias van `/personalloot`)
| Commando | Wat het doet |
|---|---|
| `/ploot info` | Info over de kist waar je naar kijkt |
| `/ploot set <loottable>` | Maak een kist persoonlijk, bijv. `simple_dungeon` |
| `/ploot reset [speler]` | Laat loot opnieuw genereren |
| `/ploot remove` | Maak er weer een gewone kist van |
| `/ploot reload` | Config herladen |

## Permissies
| Permissie | Standaard | Uitleg |
|---|---|---|
| `personalloot.admin` | op | Alle `/ploot` commando's |
| `personalloot.break` | op | Loot-kisten breken (sneaken + slaan) |

## Config
Zie `src/main/resources/config.yml`. Belangrijkste opties: `markers.style` (`frame` of `shell`), `markers.frame-block`, `markers.full-bright`, `refresh-hours`, `disabled-worlds`.

## Bouwen
GitHub bouwt de plugin automatisch bij elke push naar `main` (zie het tabblad **Actions**). De jar komt in de release van de versie uit `pom.xml`. Nieuwe versie uitbrengen: verhoog `<version>` in `pom.xml` en push.

Zelf bouwen kan ook, met **JDK 25** en Maven:
```bash
mvn package
```
De plugin staat daarna in `target/PersonalLoot-1.0.0.jar`.
