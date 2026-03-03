# WORKLOG

## 2026-03-03

### Done
- Initialized Android project scaffold with Kotlin + Compose.
- Implemented local MVP flow:
  - Import `.typ` file and display source text.
  - Import `.pdf` file and render pages with `PdfRenderer`.
  - Page navigation (prev/next).
- Added local preview usability upgrades:
  - Typst heading outline parser (`=`/`==`/`===`).
  - On-screen outline list with line hints.
  - Filename consistency hint when selected `.pdf` does not match `.typ` base name.
  - Compile button placeholder for next-stage integration.
- Added parser unit tests.
- Added Gradle wrapper and local SDK config (`local.properties`).

### Test
- Ran: `./gradlew testDebugUnitTest`
- Result: PASS

### Known Issues
- PDF is still manually selected; no automatic same-folder resolution yet.
- Compile button is a placeholder only; no Typst compile pipeline is connected.
- Current Typst display is plain text; syntax highlighting not implemented.

### Next
1. Auto-bind same-name PDF from same folder (if URI permissions allow).
2. Add basic Typst syntax highlighting in editor/reader area.
3. Replace compile placeholder with real compile pipeline (local/remote bridge).
