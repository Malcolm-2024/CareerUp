# Sprint 5 verification

Foundation: GitHub main a4f107e59bc53971db62aebf8deefd54a3f71d95.
Development: local feature/sprint5; only docs/Sprint-5 is added.

## Optional appearance draft

Dark remains the default. A small theme controller applies the saved browser
preference before React loads; a shared switch is available on sign-in and
all role pages. Light-mode overrides include controls, tables, profile dialogs,
status messages and navigation. Media is never filtered or inverted.
No database change or new dependency. Draft proposal Markdown is provided
outside this repository in the downloadable package, per the user's request.

Seventeen Node test groups passed (seven existing request/upload groups and
ten theme groups). Contrast checks cover selected normal text/palette pairs,
not full accessibility compliance. Both production builds passed; 119 Sprint 5,
84 regression, 32 upload, four database retrieval and six restart checks passed.
Four live HTTP checks confirmed the startup script, controller, CSS and React
switch are served correctly without requiring sign-in for static assets.
Actual browser appearance/playback and teammate/Mac checks remain unverified.

## Student advisor and administrator flow update

Changes: local file preview with object URL cleanup; View your post using an
optional exact-ID filter in the existing discovery endpoint; clearer labels;
sidebar action opens the collapsed form; administrator row-to-form shortcut;
advisor project-to-form shortcut selecting both student and post. Save/Send
is still required, and existing authorization and duplicate protection remain.
No libraries, tables, uploads to Git, or original Sprint 0–4 edits were added.

Final production bundle and Maven package pass. The live isolated MySQL run
passed 119 Sprint 5 checks, 84 regression checks, 32 upload checks, four direct
database retrieval checks and six restart checks. Seven frontend helper test
groups pass. The exact-ID checks cover saved ID, missing ID, wrong role and
hidden-post exclusion. An older regression assumed original posts were on the
first 50-result page; it now traverses pages as retained test data grows.

Original video problem traced to placeholder URLs in preserved sprint4_seed.sql.
The UI now distinguishes missing sample media from an actual upload/playback
failure. No original seed URL or user record was overwritten.
The in-app browser could not reach the test server; new DOM click-through,
preview rendering, scrolling/focus, real browser playback and Mac/team devices
remain manual verification items. A compiled build is not a browser test.

Verified locally against a separate MySQL 8.4.13 server and careerup_sprint5:
- React production bundle built with the existing Windows esbuild path.
- Spring Boot Maven package succeeds, targeting Java 21. Maven has no unit tests;
  the functional checks below use the live app and actual database instead.
- 116 Sprint 5 live checks: all roles, writes, invalid inputs, ownership restrictions,
  CSRF, inactive sessions, direct SQL assertions, retries and concurrent duplicate requests.
- 84 existing Sprint 4 API regression checks pass.
- 4 restart checks pass: saved post, opportunity and feedback survive app/MySQL restart;
  durable duplicate protection also survives restart.
- Launcher shutdown/start race fixed in Sprint 5 only.

Not yet verified:
- Browser interaction and visual review: the available browser could not reach
  the sandbox-local server. Vite also hit a sandbox EPERM realpath restriction;
  the existing Windows esbuild production path built successfully.
- Actual Mac startup or builds; teammate personal laptops.
- A GitHub pull of Sprint 5: this branch has not been pushed or merged.

No team meetings or teammate contributions have been fabricated.
Run the live scripts only against the private Sprint 5 test database.

## Company review shortcut

Pending company cards now open the existing decision form with that card's
request selected. Reviewed cards keep the project viewer without a review button.
The existing save refreshes the card list and shows its success message.
No new endpoint, schema, library or screen was added for this shortcut.
The production React build and Maven package pass. The final live run passed
116 Sprint 5 checks, 84 regressions, 32 upload checks, three direct database
retrieval checks and six restart checks. The seven frontend helper test groups
also passed. Browser clicking/focus/scroll behavior still needs manual checking.
An empty MySQL CLI COUNT capture recurred during an earlier run; the test helper
now retries only this read up to three times and fails if no result is captured.
No database write is repeated by that test-helper retry.

## Local file upload update

The updated backend compiled successfully. A new complete live run passed
the 116 Sprint 5 checks and 84 Sprint 4 regression checks, plus the direct
database retrieval and restart checks, against isolated MySQL on port 3395
and the application on port 8095 (the usual ports were occupied).

The upload suite passed actual PNG, MP4 and WebM multipart submissions;
MySQL ownership/media references; byte-for-byte retrieval; HTTP 206 ranges;
anonymous and wrong-role restrictions; CSRF; malformed/empty/oversized files;
blank title; sequential and concurrent retry protection with no duplicate
post. Uploaded bytes and retry receipts survived app/database restart.
Seven Node test groups passed for format/size boundaries, JSON responses,
friendly network/server/session errors and upload progress/error handling.

No new frontend dependencies or database tables were added for uploads.
Media is local disk data, excluded from Git and the ZIP. Real sample videos
were downloaded from MDN's public CC0 examples for testing only.
Browser interaction/playback, actual Mac startup and teammate machines
remain unverified; API byte/range checks do not prove browser playback.

Additional verification notes:
- Original splash/media/SQL copies match Sprint 4 byte for byte; original CSS is retained as an exact prefix.
- A concurrent row-count assertion failed once in an earlier clean-copy test. Six targeted race checks and the final full suite passed afterward; retained here for transparency.
- Local Git clone testing is blocked by the sandbox's Git/MSYS process restriction. A clean export from the committed Git tree is used to test startup without private caches instead.

Final clean-source check:
- Exported the committed Git tree into a new folder with no .local, node_modules or backend/target.
- Start-CareerUp.bat successfully used the tracked Sprint 4 Java runtime and new prebuilt Sprint 5 JAR.
- A new isolated MySQL database was created. All 116 Sprint 5 checks passed again, including concurrency.
- Three direct SQL-to-feed checks passed (inserted, hidden and draft records).
- This verifies clean Windows startup with existing MySQL tools available, not actual teammate/Mac machines or GitHub publication.
