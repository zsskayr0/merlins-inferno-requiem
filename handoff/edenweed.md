# Handoff: Edenweed

Planta mística do Hallowed Grove (folclore druídico). Itens de early game que continuam úteis fora de combate no late game.
Branch: `feature/edenweed`. Design original: seções 1-4 do handoff de game design (Edenweed).

## Implementado (C1)

| Peça | Onde | Notas |
|---|---|---|
| Arbusto `edenweed_bush` | `block/EdenweedBushBlock` | Idade 0-3, cresce por random tick (luz >= 9). Botão direito na idade 3 colhe 1-2 brotos e volta à idade 1 (rebrota). Quebrar na idade 3 dropa 1-2 brotos (`loot_table/blocks/edenweed_bush.json`). Partículas neon-verdes quando maduro. Não tem item: só é encontrado. |
| Geração | `RowanwoodTreePiece#postProcess` | Até 7 arbustos ao redor dos troncos-base (logs até 2 blocos acima da base do template), spots sorteados com seed da posição da peça, para todos os chunks da árvore concordarem. **Só árvores geradas depois desta mudança** recebem arbustos. |
| `raw_edenweed_bud` | `ModItems` | Comestível, fraco: 20s de Trance + náusea. |
| Varal `drying_rack` | `block/DryingRackBlock` + `blockentity/DryingRackBlockEntity` | 4 slots, 2 min por broto (`DRY_TICKS`). Botão direito com broto pendura; mão vazia recolhe os secos. A propriedade `contents` (empty/drying/ready) só escolhe o modelo. Receita: 5 gravetos + 1 corda + 2 tábuas. |
| `dried_edenweed` | `ModItems` | Ingrediente. |
| `wooden_pipe` | `item/WoodenPipeItem` | Durabilidade 64. Fuma 1 erva seca do inventário: Trance por 90s. |
| `edenweed_tea` | `item/EdenweedTeaItem` | Receita: erva seca + garrafa de água (`neoforge:components`). Trance por 4 min. Devolve garrafa. |
| Efeito `druidic_trance` | `effect/DruidicTranceEffect`, `event/EdenweedHandler` | Couch-lock: velocidade -35%, mineração -60%, dano -50%; resistência 80% (não vale para `BYPASSES_INVULNERABILITY`); larica: dreno de fome ~3x o Hunger I; comida dá 2x saturação e Regeneration proporcional. |
| The Sight | `client/EdenweedSight` + `mixin/MinecraftMixin` | Tudo client-side: só quem está sob o Trance vê. Entidades da tag `sight_revealed` num raio de 32 ganham outline via mixin em `Minecraft#shouldEntityAppearGlowing`. Blocos da tag `sight_revealed` num raio de 14 são contornados através das paredes. Vinheta verde nas bordas. |
| Guia | `patchouli .../entries/edenweed.json` (druid_road) + lang en_us/pt_br | |

## Decisões que não estavam no doc original
- **Rebrota** (estilo sweet berries) em vez de "quebrou, acabou", para cada Rowanwood ser fonte renovável.
- The Sight é 100% client-side (outline por jogador, não Glowing do servidor), para que uma pessoa sob o Trance não revele nada para os outros jogadores.
- Resistência 80%, não 100%: o doc pedia "altíssima", não imunidade.
- Arte é toda **placeholder gerado por script** (`textures/block/edenweed_bush_stage*`, `item/*`, `mob_effect/druidic_trance`, `gui/druidic_haze`, `drying_rack_buds_*`). Trocar pelos PNGs finais mantendo os nomes.

## Pendente
- **C2 - Incensário Pacifista** (block entity de área): queima `Compressed Edenweed Block`, fumaça ~64x64, impede spawn hostil e torna hostis/bosses neutros dentro da área. Ainda não existe o `Compressed Edenweed Block` nem o Incensário.
- **C3 - Óleo Estabilizador**: erva prensada -> `Edenweed Essential Oil`, base dos caldeirões de poções de late game, com explosão sem o óleo (vai exigir mixin/evento no caldeirão).
- Sons próprios (fumar, secar, colher); hoje usa vanilla.
- Valores a calibrar: `DRY_TICKS`, duração do Trance, `HUNGER_EXHAUSTION`, raios da Sight, lista das tags `sight_revealed`.
- Bloco do arbusto não tem item; se quiser plantar/bonemeal, definir semente.
