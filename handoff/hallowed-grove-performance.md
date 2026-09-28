# Hallowed Grove — nota de performance

**Origem:** ao entrar no bioma num modpack maior (Quark + Create) houve queda perceptível de performance. Não se sabe se foi um spike isolado ou algo recorrente. **Nada foi medido ainda** — o que segue é análise estática do código, ordenada por probabilidade. Nenhuma alteração de código foi feita.

## O bioma em si está limpo
`ModBiomeProvider` usa só features vanilla (flores, grama, cogumelos, berries, ores, carvers). Não é a fonte do problema.

## Suspeitos (mais → menos provável)

### 1. Geração de chunk ao chegar no bioma (mais provável, dado "assim que cheguei")
Chunks novos do Grove fazem colar NBTs grandes, o que explicaria um spike ao entrar e que passa depois de explorar:
- **Rowanwood** (`RowanwoodTreeStructure`): estrutura de ~48×37×51 (NBTs de 34–43 KB), sem `terrain_adaptation`, espalhada por vários chunks; tudo folha/tronco. Copas grandes = muito trabalho de iluminação (folha atenua luz). Spacing 24/separation 10 → 1 a cada ~24 chunks, mas quando cai no raio de geração o custo é alto.
- **Ashwood** (`StructurePasteFeature`): 2–3 tentativas por chunk (`count` 2 peso 9 / 3 peso 1), cada uma faz varredura de altura na footprint + varredura de "árvore existente" (com margem `TREE_CLEARANCE`) + `placeInWorld`. ~48 variantes de NBT: **primeira leitura de cada uma do disco** também causa spike pontual (`getOrCreate` cacheia depois).
- **Modpack:** Quark e Create adicionam features/estruturas e mixins de worldgen próprios; o custo soma com o nosso e o bioma é pesado justamente em chunk-gen.
- **Sanctuary** (`DruidSanctuaryStructure`): 10 chamadas `getFirstOccupiedHeight` por candidato (2 por amostra × 5), só custa em chunks candidatos. Baixo.

### 2. Folhas coladas do NBT do Ashwood (a verificar)
Os NBTs do Ashwood vêm de spruce e o `BlockPaletteSwapProcessor` copia as propriedades. Se as folhas do NBT tiverem `persistent=false`, elas entram no sistema de decay (random tick + propagação de `distance`) — comportamento vanilla, mas em copas grandes e muitas árvores pesa. Rowanwood já está com `persistent=true` (documentado no código). **Checar os NBTs do Ashwood.**

### 3. Custo em runtime das entidades (baixo)
- `DullahanEntity` / `OstaraEntity`: checagem de bioma só a cada 20 ticks (`shouldStayActive`), Ostara não faz nada caro por tick. OK.
- **Regra de spawn:** `getEntitiesOfClass` com raio **128** (Dullahan) e **256** (Ostara) a cada tentativa de spawn que sorteia essas entidades. É uma consulta de ~17×17 (ou 33×33) chunks. Só roda quando o sorteio cai nelas (pesos 5 e 2 contra ~500), então é raro — mas é o único ponto de custo repetitivo no runtime do bioma.

## Como confirmar antes de mexer
1. Reproduzir num mundo novo: teleportar para chunks **nunca gerados** do Grove vs. chunks já gerados. Se só os novos travam → é chunk-gen (itens 1/2). Se os já gerados também → runtime (item 3) ou mods.
2. `spark` (profiler) com `/spark profiler start --timeout 60` durante a chegada ao bioma; olhar o thread do servidor e worldgen (`Worker-Main`) por `StructurePasteFeature`, `RowanwoodTreePiece`, lighting.
3. Repetir sem Quark/Create para isolar a contribuição do modpack.

## Ideias de correção (só depois de medir)
- Pré-carregar/cachear os templates do Ashwood no startup em vez de na primeira geração.
- Reduzir `count` do Ashwood ou aumentar o filtro para falhar cedo (barato antes do `getOrCreate`).
- Rowanwood: reduzir footprint/variantes, ou mais spacing.
- Dullahan/Ostara: raio de exclusão menor, ou checar `dayTime`/bioma antes do `getEntitiesOfClass` (Dullahan já faz nessa ordem; Ostara também).
