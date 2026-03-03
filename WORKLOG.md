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
  - Import `.typ` 后自动给出同名 PDF 推荐名（`xxx.pdf`）。
  - PDF 导入按钮在有 typ 后变为“选择同名 PDF”。
  - Compile button placeholder for next-stage integration.
- Added render error classification for PDF preview:
  - Open failed (permission/file open failure)
  - Page out of range
  - File corrupted
  - Unknown error
- Added minimal UI polish for usability:
  - Dedicated status bar block
  - Current selected typ/pdf filename display
  - Prev/next page buttons now have disabled state at boundaries
  - Empty preview state now includes direct import action and clear guidance text
- Added heading-to-source jump behavior (outline click scrolls editor area to target line).
- Added restore-last-session behavior:
  - Persist typ URI, pdf URI, and last PDF page index.
  - Recover saved state on next app launch.
- Added PDF neighbor-page prefetch cache (current page +/- 1).
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
