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
  - Validate saved URIs at startup; auto-clear invalid entries and prompt user to re-import.
- Added PDF neighbor-page prefetch cache (current page +/- 1).
- Added export action for current PDF page to PNG (saved under app external files export folder).
- Added unit tests for restore-status messaging branches.
- Refactored `MainActivity` by extracting state/constants, file access helpers, and PDF utilities into dedicated files.
- Extracted UI components into dedicated modules:
  - `ui/components/StatusBar.kt`
  - `ui/components/PdfPageImage.kt`
  - `ui/components/TopActionsBar.kt`
  - `ui/components/OutlinePanel.kt`
  - `ui/components/SourceViewer.kt`
  - `ui/components/PdfPreviewPanel.kt`
- Added Typst text search workflow:
  - keyword input panel (`ui/components/SearchPanel.kt`)
  - previous/next match navigation with source scroll jump
  - line-match helper (`TypstSearch.kt`) and unit test coverage (`TypstSearchTest.kt`)
- Stabilized typ/pdf pairing flow:
  - importing a new `.typ` now clears stale loaded PDF to avoid mismatched preview context
  - export filenames now include timestamp suffix to avoid overwrite conflicts
- Added pluggable compile workflow:
  - compiler abstraction (`TypstCompiler`) and local command bridge (`LocalTypstCommandCompiler`)
  - compile button now executes real compile flow, auto-loads generated PDF on success, and reports explicit failure reasons
  - compiler availability check added (`typst --version`) with clear fallback prompt when command is missing
- Improved local generated PDF compatibility (`file://`) for page counting/rendering.
- Improved `.typ` import compatibility for QQ/files app workflows:
  - typ picker now uses broad filter (`*/*`) so non-standard providers can be selected
  - extension whitelist check (`.typ/.txt/.md`) with explicit user guidance for QQ downloads
  - added filename utility tests
- Validation pass completed with `./gradlew testDebugUnitTest assembleDebug`.
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
