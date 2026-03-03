# Typst Android Previewer (Kotlin, native-first)

A starter Android app (Kotlin + Compose) focused on your core requirement:
- Import `.typ`
- Preview corresponding `.pdf`

## Current MVP
- Import Typst source file via system picker
- Read and display Typst text
- Import PDF and preview pages with next/prev controls (PdfRenderer)

## Why this architecture
- Native Android UI (no WebView editor)
- Keep editing and rendering decoupled
- Can evolve into: local compile bridge / remote compile service

## Build
Open in Android Studio and sync Gradle.

## Next milestones
1. Syntax highlight for Typst
2. TOC extraction from heading lines
3. Bind `.typ` and `.pdf` by filename
4. Add compile action hook (local/remote)
