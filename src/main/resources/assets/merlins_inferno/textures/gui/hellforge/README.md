# Hell Forge GUI textures

No new GUI art was needed for the multiblock refactor: the Controller opens the same GUI as before.
These existing, final files are reused untouched:

| File | Size | Used for |
|---|---|---|
| `hell_forge.png` | 256x256 sheet, 176x166 panel in the top-left | Panel background |
| `hell_forge_bar_fill.png` | 172x20 | Fuel tank bar |
| `hell_forge_burn_p0.png` .. `p10.png` | 16x16 each | Cook progress icon (p10 idle, p0 done) |

There is no stage/Core indicator in the GUI yet (the Demonic Core is the only Core). When more Cores exist,
its art (suggested: a small icon next to the cook icon) is still to be provided.
