# Handoff: estado do mod (para brainstorm)

**Merlin's Inferno: Requiem** · NeoForge 1.21.1 (21.1.249) · GeckoLib 4.8+ (obrigatório), Patchouli (opcional) · `0.0.1-alpha` · en_us + pt_br
Branch `main`, último merge: PR #5 (Vigil Shrine). Único diff pendente: `textures/block/netherrack_compressed.png`.
Data desta foto: 2026-09-26.

## Conceito
Três "estradas" de equipamento, cada uma com origem, criaturas e estilo próprios. Cada origem deve ter, por tier, **3 materiais + 1 arma lendária** (craftada ou encontrada) — decisão de design já tomada.

| Estrada | Origem | Set | Identidade |
|---|---|---|---|
| Druídica | Overworld / Hallowed Grove | Rowanwood | Mina como diamante, dano de ferro, durabilidade > diamante. +30% vs. mágicos, −30% vs. animais comuns |
| Infernal | Nether | Demonblood | ~Netherite com mais encantabilidade. +10% vs. humanos. Armadura *Demon's Fury*: fogo −50%, dano hostil −25%, neutros +25% |
| Angelical | Geodos de Lyrium (montanhas) | Seraphium | O mais forte e mais difícil. Cada golpe aplica Sanctified; muito dano a demônios/hostis, pouco a neutros/pacíficos |

## O que existe hoje

### Overworld / Druídica
- **Hallowed Grove** (bioma via mixin no `OverworldBiomeBuilder`): Ashwood (madeira de construção, set completo) e Rowanwood (árvore rara, gigante).
- **Santuário do Druida** (estrutura): clareira com pilares, lareira, baú, Druida.
- **Druid** (NPC pacífico): caça mortos-vivos, foge do Dullahan; troca 20 esmeraldas por Mundane Essence.
- **Dullahan** (miniboss noturno, 250 HP / 15 dano, GeckoLib, montaria espectral, boss bar verde, lento, tag undead): morta a montaria, luta a pé. Drop: 2–3 Fae Essence.
- **Druid's Touch** (encantamento): sem ele, logs de Rowanwood dão só Ashwood.
- Materiais: Mundane/Fae/Otherworld Essence → Rowanwood Scrap → Bar (fundição) → ferramentas + armadura.

### Nether / Infernal
- **Imp** (voo, GeckoLib, IA em grupo): sozinho foge, em bando (3+) mergulha e rouba ouro. Drops: Demon Blood etc.
- **Starved** (miniboss quadrúpede, 80 HP / 8 dano base, GeckoLib): mecânica de **Hunger** (0–10 stacks a cada 4s sem acertar; dano e brilho escalam; acertar zera e cura) e **Charge** (a partir de 4 stacks: windup 1,25s + dash). Nunca foge. Drops: Demon Blood, Infernal Sinew. Spawn via biome modifier.
- **Hell Forge**: multiblock com GUI, queima balde de lava/pó de blaze, único lugar onde Demonblood Scrap vira Bar.
- Encantamentos **Bane of Humanity** e **Evil**. Ore: Demonite Debris. Netherrack comprimido.
- Materiais: Demon Blood, Infernal Sinew, Withered Bone (esqueleto do Wither), Infernal Essence (barter com piglin) → Scrap → Bar → ferramentas + armadura.

### Angelical
- **Lyrium**: geodos (irradiam luz e incendeiam com Sanctified mortos-vivos/demônios próximos) + minério espalhado. Cadeia: Raw → Impure → Refined → Pure → True. Só Raw→Impure→Refined têm uso; **Pure e True estão registrados sem função** (ganchos p/ conteúdo futuro). Refined vem de trade com Cleric mestre ou loot de pillager.
- **Sanctified** (efeito): queima como veneno e **pode matar**; portador bate mais em mortos-vivos e espalha o efeito. Regra geral: mobs nunca espalham Sanctified.
- **Vigil Shrine** (estrutura) + **Vigil Altar** (bloco): diamante no altar acorda o **Penitent**; morte inicia cooldown.
- **Penitent** (miniboss, 250 HP / 15 dano, lento, corrente com alcance extra): **Grande Sino** toca a cada 1 min em combate; cada badalada o fortalece e aumenta regen, mas ele toma mais dano; na 7ª badalada ou <30% HP o sino quebra → **fúria** (rápido, forte, sem regen, dano normal). Exceção deliberada: cada golpe dele sobe Sanctified em um nível (leite limpa). Drop: Celestial Essence.
- Seraphium: refined Lyrium + diamante + Celestial Essence → Scrap → Bar → ferramentas + armadura.
- **Aldeões**: `LyriumVillagerTrades` (Cleric).

### Infra / extras
- **Grimmorium** (item-livro), Patchouli guide com entradas para quase tudo (ashwood, demonblood, druid, druids_touch, dullahan, geodes, hallowed_grove, hell_forge, imp, lyrium, penitent, pillars, rowanwood, sanctified, seraphium, starved, vigil_shrine, welcome), advancements por armadura.
- Compat: tags `c:`, receitas por tag, handler de minérios do Druid's Touch.
- Datagen (blockstates, loot, worldgen). Config COMMON existe mas está **vazia**.
- Áudio: `ModSounds`, `sounds.json` e pasta `sounds/entity` já existem; briefing de áudio do Starved em [starved-audio.md](starved-audio.md) (a integração pode estar parcial — conferir).

## Lacunas e pendências conhecidas
1. **Armas lendárias**: nenhuma implementada. A primeira planejada é a espada do Overworld/Druida, com materiais a 1% de drop do Dullahan. **O Dullahan já está "cheio" de loot** — não empilhar mais drops nele (ver notas de memória); precisa de outra fonte para o livro do Druid's Touch (ex.: tag `magical_mobs` com chance baixa).
2. **Seraphium**: obtenção de Celestial Essence agora tem o Penitent, mas o comentário TODO em `ModItems` ainda diz "deliberate TODO".
3. **Lyrium Pure/True**: sem receita/uso; pensados como conteúdo mid/end-game (estrutura/máquina de processamento ainda indefinida).
4. **Boss final do Nether**: o Starved foi descrito como "entre o Imp e o boss final" — o boss final não existe.
5. **Tiers/mundos**: falta um registro "tier → origem → (3 materiais + 1 lendária)" e o Grimmorium modelando isso.
6. **Áudio custom**: Starved (e provavelmente os outros mobs) ainda usa sons vanilla como placeholder em parte.
7. **Simetria de conteúdo**: Druídica tem Dullahan (noite, mundo aberto), Infernal tem Starved (miniboss errante), Angelical tem Penitent (arena com altar). Só a Angelical tem estrutura-boss "invocável"; Infernal não tem estrutura própria.
8. **Config**: nada configurável (spawn rates, dano de miniboss, cooldown do altar, etc.).

## Perguntas em aberto para o brainstorm
- **Lendárias**: qual a arma de cada origem/tier, e como ela é obtida (craft com 3 materiais raros vs. achado em estrutura)? Elas têm habilidade ativa/passiva ou só stats?
- **Tiers**: o que vem *depois* dos três sets atuais? Um tier 2 por origem reaproveita as mesmas criaturas ou pede novas?
- **Boss final do Nether**: quem é, onde vive, como se relaciona com Imp → Starved? Espelhos para Overworld e Angelical?
- **Estrutura Infernal**: o Nether merece um equivalente ao Santuário/Vigil Shrine (ex.: onde o Starved é "guardião" ou onde a Hell Forge é obtida)?
- **Lyrium Pure/True**: que mecânica os consome (altar, máquina, ritual do Vigil)?
- **Progressão cruzada**: as estradas são exclusivas ou há sinergias/penalidades ao misturar sets (ex.: Sanctified vs. Demon's Fury)?
- **Balanceamento**: minibosses estão em 80–250 HP; para onde escala o "tier 2"?
- **Onde Druid's Touch obtém o livro** sem tocar no Dullahan.

## Referências de código
- Registros: `registry/` (`ModItems`, `ModBlocks`, `ModEntityTypes`, `ModEffects`, `ModEnchantments`, `ModSounds`, …)
- Mobs: `entity/{Imp,Starved,Dullahan,DullahanSteed,Druid,Penitent,Worshipper}Entity.java`, IA em `entity/ai/`
- Combate/eventos: `event/` (Demonblood, Rowanwood, Seraphium, Sanctified, DruidsTouch, LyriumVillagerTrades)
- Worldgen: `worldgen/` (estruturas `DruidSanctuary*`, `VigilShrine*`, `RowanwoodTree*`, `GreatBell`)
- Dados: `src/main/resources/data/merlins_inferno/` (loot_table/entities, recipe, biome_modifier, patchouli_books)
