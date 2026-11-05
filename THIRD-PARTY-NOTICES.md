# Third-party notices

This app bundles two font binaries and links a number of Apache-2.0 libraries. Their licences and
copyright notices are reproduced here, which is what the SIL Open Font License requires of anyone
redistributing the font software.

---

## Fonts (bundled binaries redistributed in this repository)

### Google Sans Flex

    Copyright 2015 Google LLC

- **Licence:** SIL Open Font License 1.1. See [`licenses/OFL-1.1.txt`](licenses/OFL-1.1.txt).
- **Where it came from:** `fonts.gstatic.com`, via the Google Fonts CSS API. Google Fonts lists the
  family as open source (`isOpenSource: true` in its own family metadata).
- **Modified: yes.** `tools/build_gsflex.py` downloads the per-script woff2 subsets Google serves,
  converts them to TTF and merges them into one variable font, because Android cannot load woff2 and
  no single subset covers this app's needs. Glyph outlines, the design space and all six variable
  axes are unchanged; only the set of glyphs present differs from any one upstream subset.
- **No Reserved Font Name is declared** in the binary's name table, so OFL §3 places no restriction
  on the family name. The modified font therefore keeps its original name, as the licence permits.

### Noto Sans Arabic

    Copyright 2022 The Noto Project Authors (https://github.com/notofonts/arabic)

- **Licence:** SIL Open Font License 1.1. See [`licenses/OFL-1.1.txt`](licenses/OFL-1.1.txt).
- **Modified: yes.** `tools/build_noto_digits.py` cuts a ten-glyph subset (U+0660–0669, the Eastern
  Arabic digits) for the clock widget. Google Sans Flex has no Arabic coverage at all, which is why
  a second font is bundled for ten glyphs.

---

## World Clock map

The precalculated dotted map in `WorldMapData.kt` was derived using
[NTag/dotted-map](https://github.com/NTag/dotted-map), copyright (c) 2021 Basile
Bruneau, under the MIT Licence. The full copyright and permission notice is
included in [world-map-notices.txt](app/src/main/assets/licenses/world-map-notices.txt),
which is also packaged in the application assets.

Made with [Natural Earth](https://www.naturalearthdata.com/). Its underlying map
data are [public domain](https://www.naturalearthdata.com/about/terms-of-use/);
credit is provided voluntarily.

---

## Libraries (linked, not redistributed here)

All under the Apache License 2.0:

- Jetpack Compose and AndroidX: Copyright The Android Open Source Project
- `androidx.compose.material:material-icons-extended` is the Material Symbols icon set as Compose
  `ImageVector`s. Copyright Google LLC / The Android Open Source Project.
- `androidx.graphics:graphics-shapes`: Copyright The Android Open Source Project
- Kotlin and the Kotlin standard library: Copyright JetBrains s.r.o.

Gradle resolves these from Maven Central and Google's Maven repository at build time; no copy of any
of them is stored in this repository.

---

## Trademarks

Google, Google Sans, Material, Material Design, Android and Pixel are trademarks of Google LLC.
This project is not affiliated with, sponsored by, or endorsed by Google.

Google Sans Flex is one of Google's **brand** typefaces (Google Fonts flags it `isBrandFont: true`).
Its *font software* is open-licensed and may be redistributed under the OFL, as above; that licence
grants no rights in the Google name or in any Google trademark, and nothing here should be read as
claiming any.

---

## What this app is a recreation of

The design follows Google's unshipped **Material 3 Expressive research-concept clock**. The
proportions in the source comments were measured off Google's published concept renders. Those
renders are Google's copyright and are **not** included in this repository, which holds only the
measurements taken from them, and the code that reproduces the design. Comments that cite
`clock-mockups/` or `docs/m3/` refer to material outside this repository.
