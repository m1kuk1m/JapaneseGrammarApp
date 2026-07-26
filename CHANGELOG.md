# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.9.0] - 2026-07-26

### Added
- Per-module model configuration: each analysis module (tokenizer variants, segments, translation, clauses, grammar) can be pointed at its own provider and model, falling back to the global setting when left blank.
- `AnalysisModule` enum as the single source of truth for the analysis pipeline, with stable ASCII identifiers used for preference keys and API log labels, decoupled from UI display names.

### Changed
- API debug logs now label entries by module identifier, with backward-compatible resolution of the previous Japanese labels.
- Consolidated 23 per-version release note files into this changelog.
- Rewrote README: corrected the Compose version (the previously listed 1.5.4 is the Kotlin compiler extension, not the library version), removed a reference to an OkHttp logging interceptor that is not a dependency, and qualified the credential-storage claim (`EncryptedSharedPreferences` does not protect against an attacker with root).

### Fixed
- Replaced three hardcoded FileProvider authority literals with `${context.packageName}.fileprovider`, matching the pattern already used elsewhere and preventing silent runtime breakage if the application ID changes.

### Removed
- Deleted `.gemini_scratch_prompt.txt`, an unreferenced and mojibake-corrupted stale copy of `PromptManager.kt`.

## [1.8.1] - 2026-07-24

### Added
- Volume key capture support and four-edge drag crop support in camera screen.
- Recorded and displayed reasoning level (CoT depth) in API debug logs.

### Changed
- Optimized CoT reasoning depth settings with a floating dialog and enhanced API provider model refreshing UX.
- Removed hardcoded `json_object` response_format from OpenAI non-streaming calls and hardcoded `application/json` responseMimeType from Gemini calls.
- Reorganized endpoint action buttons with responsive wrapping layout to avoid text clipping.

### Fixed
- Resolved prompt editor magnifier freeze on downward text drag, and preserved selection/scroll position using `TextFieldValue` to prevent auto-scrolling to top.

## [1.8.0] - 2026-07-07

### Added
- LLM Chain-of-Thought (CoT) enhancements: component-specific CoT depth sliders in settings; captured and logged AI reasoning processes and token usage in API logs; automatically extracted and cleaned inline `<think>` tags from model responses; supported capturing reasoning text from Gemini requests via `includeThoughts` and handled implicit reasoning token scenarios.
- New setting to automatically strip accidental spaces in Japanese input prior to AI analysis.
- Reasoning level configuration.

### Changed
- Made backup API usage, automatic retry, and endpoint failover optional and fully localized in settings.

### Fixed
- Fixed `modelUsed` tracking on retry/re-analysis.

## [1.7.3] - 2026-07-03

### Changed
- Improved tokenizer normalization when LLM/OCR output returns Japanese punctuation attached to words.
- Split safe punctuation such as Japanese ellipses and brackets out of mixed tokens before analysis.
- Preserved the original token surface text during segment reconstruction when the analyzer omits punctuation.

This release should make grammar analysis more stable for sentences containing ellipses, quotes, brackets, and punctuation-heavy OCR/LLM outputs.

## [1.7.2] - 2026-07-02

### Added
- Prompt preset import/export support and related settings workflow improvements.

### Changed
- Refined prompt/settings UI text and localization across English, Japanese, and Chinese resources.
- Updated bookmark, workspace, and statistics screens to work with the enhanced prompt preset data model.

### Fixed
- Fixed failed analysis tasks so progress tracking is finished before the record is marked failed.

## [1.7.1] - 2026-07-01

### Added
- New UI smoke test (`AppUiSmokeTest.kt`) and test cases for popup avoidance math.

### Changed
- Forced the sentence analysis floating window to always display below the anchor text, adjusting layout positioning to ensure accessibility and readability when space is tight.
- Upgraded image tokenizer prompts to support vertical reading order (right-to-left, top-to-bottom) for Japanese text.

### Fixed
- Improved statistics screen navigation behavior from bookmarked sheets, resolved UI state transitions, and added testing tags for reliable automated smoke test coverage.

## [1.7.0] - 2026-06-30

### Added
- Comprehensive word segment customization settings including font size, spacing, card internal padding, and furigana vertical gap scales, complete with localized strings (ZH/JA) and a real-time interactive preview.
- Card detail display mode settings, allowing users to toggle between Inline view and Anchored Floating Dialog view.

### Changed
- Refactored the floating card detail popup to be anchored and compact with a hybrid aesthetic, smart auto-positioning, smooth scale and fade transition animations, and custom soft shadows.
- Optimized WorkspaceScreen exit/collapsing animations for better synchronization, and added a top fade-out gradient to the history sidebar.

### Fixed
- Fixed the system gesture exclusion bug on Android 10+ (API 29+), allowing system edge-swipe back navigation to pass through correctly when the floating dialog is visible.
- Fixed sentence card top gap layout issues.

## [1.6.0] - 2026-06-30

### Added
- Daily, weekly, monthly, and yearly learning statistics with interactive charts, heatmap, and review features, localized in ZH and JA.
- Dynamic collapsing headers, expandable sentence cards, and sticky toolbars with smooth transitions in the workspace.

### Changed
- Upgraded Compose BOM, optimized duplicate check queries with database indexing, and migrated schema to v15.
- Reorganized settings into 6 logical categories, stabilized tokenizer/LLM streaming error propagation, and added robust fallback handling.

### Fixed
- Refined scroll position reset, eliminated drift during transition, resolved scrolling freezes, and optimized sidebar closing speed.

## [1.5.0] - 2026-06-26

### Added
- Vertical drag navigation (up/down/next/prev) to history records and automatic scroll-to-top on selection.
- Full-Text Search (FTS) and indices to Room database for high-performance record lookups.
- Year/month tag filters and archiving filters for sentences and grammar bookmarks.
- Full edit capabilities for custom word segments and bookmarked words.

### Changed
- Reversed the history sidebar order, added unread indicators for new records, and optimized the history drawer export range/filters.
- Redesigned settings page into a clean two-level directory structure and unified root menu styling.
- Refined tokenizer prompts by removing particle splitting rules to improve token boundary accuracy.

## [1.4.11] - 2026-06-26

### Changed
- Optimized Japanese tokenizer and OCR image repair tokenizer prompts to prevent token splitting during line breaks, ensuring consistent text segmentation boundaries.

## [1.4.10] - 2026-06-25

### Changed
- Standardized and refined prompt instructions for Japanese tokenizer and OCR image repair tokenizer to ensure consistent, high-accuracy text segmentation boundaries.

## [1.4.9] - 2026-06-25

### Changed
- Optimized the part-of-speech coloring scheme (including new tags/colors for Affixes and Phrases, and mapping for Pronouns/Interjections).
- Improved visual representation of selected and bookmarked word segment chips with enhanced borders.
- Refined and simplified prompt segmentation rules in `PromptManager` to improve accuracy of token boundaries.

### Fixed
- Fixed the image rotation canvas rendering issue where rotating the cropped image caused incorrect visual offsets.

## [1.4.8] - 2026-06-24

### Changed
- Optimized prompt rules to ensure the LLM outputs verbs and their auxiliary verb conjugations as a single token rather than splitting them into separate lines.

## [1.4.7] - 2026-06-24

### Added
- Auto-cropping with dynamic padding for image-based text selection.
- Camera auto-calibration setting to automatically adjust image skew after capturing.

### Changed
- Optimized LLM prompts for translation and token analysis, reducing redundant instructions and token consumption.

## [1.4.6] - 2026-06-23

### Fixed
- Fixed a bug where a separator line (vertical bar) appeared inside/after the "文中の役割" (Role in Sentence) field in word analysis details.

## [1.4.5] - 2026-06-23

### Changed
- Optimized Japanese tokenization prompts to improve particle separation accuracy, especially for consecutive particles and hiragana-only contexts.

## [1.4.4] - 2026-06-22

### Changed
- Improved OCR text region bounding box height calculations: bounding boxes for short or thin text lines are now significantly taller and more accurate.
- Hidden bottom control panel while dragging selection areas in the crop screen to prevent layout obstruction.

### Fixed
- Fixed toast notification visibility issue: saved prompt success notifications are now immediately visible over the `SettingsPromptEditor` overlay.

## [1.4.3] - 2026-06-22

### Fixed
- Fixed magnifier positioning and offset bugs in landscape orientation for image cropping and text selection.
- Fixed text selection start handle alignment and offset issues.
- Fixed bottom button overlap and layout bugs in the camera crop review screen.

## [1.4.2] - 2026-06-21

### Fixed
- Fixed a UI bug where the drop shadow of the expanded analysis card was clipped.

## [1.4.1] - 2026-06-21

### Added
- Multi-format export and import: JSON for complete backups (backup/restore), standard CSV for spreadsheet viewing/editing, and customized TSV designed for Anki card deck importing.
- Granular data selection: selectively import or export Word, Sentence, and Grammar Point bookmarks.
- Conflict resolution strategies: handle duplicate items with "Skip existing" or "Overwrite existing".
- Comprehensive import summary showing total successfully imported, skipped (duplicates/empty), and failed bookmarks, with failure reasons for troubleshooting.
- Dedicated "Import" and "Export" action buttons and confirmation dialogs in the Bookmarks screen.

### Changed
- Full localization: all import/export dialogues, option menus, conflict strategies, and summary reports translated into English, Japanese, and Simplified Chinese.

## [1.4.0] - 2026-06-20

### Added
- Grammar Point Bookmarking system, completing the bookmarks trifecta (Sentences, Words, and Grammar). Includes a new "Grammar" tab on the Bookmarks screen and flashcard study/practice modes for saved grammar concepts.
- Grammar bookmarking star toggle integrated directly into the Workspace sentence analysis cards, enabling one-tap saving of grammar points as they are analyzed.
- Upgraded `AppDatabase` to version 10, including an automated migration path (Migration 9->10) and migration test cases to safely store the new grammar bookmark entities.

### Changed
- Applied `animateContentSize()` transitions on sentence analysis result cards to prevent jarring page layout jumps when parsing finishes.
- Integrated a `Crossfade` transition between the Text-to-Speech play and stop buttons.

### Fixed
- Replaced the abrupt disappearance of the sentence analysis loading indicators with smooth fade-in/fade-out animations.

## [1.3.0] - 2026-06-19

### Added
- Multi-Preset Prompt Management: `PromptPreset` system supporting custom prompt schemes. Users can create, rename, and delete personalized LLM prompt configurations for Grammar, Vocabulary, and Translation dynamically in Settings.
- Sub-Line Text Selection & Masking: `MaskedCropHelper` extracts target text regions onto a clean white canvas, isolating selected text from background noise. `TextSelectionMath` handles sub-line range calculations for vertical/horizontal Japanese scripts.
- Robust App Logger: `AppLogger` persists and recovers logs across application sessions, enabling diagnostic copy-paste even after crash restarts.

### Fixed
- Camera viewport alignment: bound preview and capture use cases to a unified `UseCaseGroup` viewport in `CameraPreviewLayout`, resolving the aspect-ratio mismatch where final captured images were larger than the interface preview.
- Fixed full API response text truncation issue in the review dialog layout.
- Rectified uneven button dimensions caused by long translated strings in Chinese/Japanese locales, and cleaned up phrasing in translations to maintain a compact, native look.

## [1.2.1] - 2026-06-19

### Fixed
- Fixed token card layout animation and missing punctuation display.
- Fixed missing star icon for bookmarked words.
- Improved background analysis task handling.

## [1.2.0] - 2026-06-18

### Added
- Real-time LLM streaming response support with typing animations.
- Comprehensive Sentence and Word Bookmarking system, with TabRow switcher and flashcard practice.
- Import/export capability for bookmark history.
- Multi-language localization (i18n) support across the entire app.
- Landscape camera support and improved cropping UX.
- Custom TTS provider support.

### Changed
- Decoupled database schema for better performance.
- Navigation stack and database repository optimizations.

### Fixed
- Fixed loading animations, UI shimmers, and keyboard auto-popup bugs.
- Various UI cleanups and bug fixes.

## [1.1.1] - 2026-06-15

### Added
- Floating action ball for quick text selection.
- Smart OCR bounding box clustering with generous padding.
- Invisible edge swipe interceptor for better Drawer gesture handling.

### Fixed
- Refactored layout to wrap Pager with Drawer to fix native gesture conflicts.
- Resolved OCR over-merging issues.

## [1.1.0] - 2026-06-15

### Added
- Complete redesign of the homepage, cards, and theme color decoupling for a minimalist zen aesthetic.
- Auto-fetch supported models from API providers in Settings.
- Camera aspect ratio matching and pinch-to-zoom in the crop selection box.

### Changed
- Split monolithic LLM prompts into 4 specialized parallel API calls for faster and more accurate analysis.
- Refactored Part-of-Speech (POS) coloring to use strictly typed enums.
- Decoupled TTS settings from the Main Thread to fix scrolling stutter.

### Fixed
- Implemented safe image downsampling to prevent Out-Of-Memory crashes.

## [1.0.0] - 2026-06-08

### Added
- Initial stable release featuring Japanese grammar analysis using Kuromoji tokenizer and custom LLM prompts.

[1.9.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.9.0
[1.8.1]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.8.1
[1.8.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.8.0
[1.7.3]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.7.3
[1.7.2]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.7.2
[1.7.1]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.7.1
[1.7.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.7.0
[1.6.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.6.0
[1.5.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.5.0
[1.4.11]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.11
[1.4.10]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.10
[1.4.9]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.9
[1.4.8]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.8
[1.4.7]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.7
[1.4.6]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.6
[1.4.5]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.5
[1.4.4]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.4
[1.4.3]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.3
[1.4.2]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.2
[1.4.1]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.1
[1.4.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.4.0
[1.3.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.3.0
[1.2.1]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.2.1
[1.2.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.2.0
[1.1.1]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.1.1
[1.1.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.1.0
[1.0.0]: https://github.com/m1kuk1m/YomiLLM/releases/tag/v1.0.0
