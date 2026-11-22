# CareerUp Sprint 5

Sprint 5 continues the working Sprint 4 React / Spring Boot / MySQL application.
The Sprint 0–4 folders are unchanged. All Sprint 5 work is in this folder.

## DEMO VIDEO/ RUNTHROUGH

https://youtu.be/EcDb0dnCPFw?is=kWvv10QU3BJe-DUa

## Run

Windows: double-click **Start-CareerUp.bat**. In a Git checkout, it can reuse
Java already included under docs/Sprint-4/deliverables/runtime.
Mac: open **Start CareerUp.command**, or run `bash "Start CareerUp.command"`
from this folder. The existing automatic Mac setup requires macOS 15+.

The default Sprint 5 address is http://127.0.0.1:8085 (the launcher may choose
another free port on Windows). Its separate MySQL database is careerup_sprint5,
normally on port 3315. Sprint 4's database is not changed.
First startup may download verified vendor tools if they are missing.

## Sign in and demonstrate

Password for the demo accounts: **CareerUp4!**

- admin@careerup.demo: open Account status; select another user; activate/deactivate.
- student@careerup.demo: open Create portfolio post; enter a title and description,
  then choose a picture/video from the computer (or use a direct media link).
  Publish. The discovery feed refreshes.
- recruiter@careerup.demo: open Create opportunity; complete the form and publish.
  Saved opportunities appear below the form.
- company@careerup.demo: open Review verification; select a pending Delta request
  and approve or reject. The request list shows the saved decision.
- advisor@careerup.demo: open Give feedback; select Nia and a portfolio post;
  enter feedback and send. Feedback is visible to the advisor and the student
  (student opens the portfolio action panel to see received feedback).

The original Sprint 4 retrieval screens remain available below these actions.
Shortcuts: Change status beside a user; Review request on a pending company
card; Give feedback beneath a project in the advisor's student profile.
Each shortcut selects the record in the existing form; Save/Send confirms it.
Students can preview a chosen file and use View your post after publishing.
The form starts closed so the discovery feed stays the focus when browsing.

## Edit / pull / rebuild

Use IntelliJ with the whole repository open. Sprint 5 code is under
`docs/Sprint-5/deliverables/frontend` and `backend`.
Pull using `git pull --ff-only` from the repository root, on the team's agreed
branch, after committing your own edits. Never force a push.
Then run **Team Tools / Rebuild CareerUp.cmd** (Windows) or
**Rebuild CareerUp.command** (Mac). Refresh the browser after it finishes.

This branch has NOT been pushed. A GitHub pull of Sprint 5 is not available
until the team approves publishing it. Teammate and actual Mac runs still need testing.

## Show the real database

Keep Sprint 5 running. Add an IntelliJ MySQL data source using this computer's
`.local/IntelliJ MySQL Connection.txt`. Keep that file private and out of Git.
Inspect users.status, posts/post_media, jobs, and advisor_notes before/after saving.
Use only this Sprint 5 database for these demos; do not rerun original Sprint 3 seeds.

## Automated live tests

With Sprint 5 running, Python 3 and MySQL client tools available:
set CAREERUP_URL to the actual application URL and MYSQL_HOME to the MySQL tools
folder. Run `python scripts/test_sprint5.py` (writes test rows only to careerup_sprint5).
For Sprint 4 regressions, run `python scripts/test_api.py` with the same CAREERUP_URL.
Test rows are retained for inspection. No passwords are printed.

## Pictures and videos

Pictures: JPG, PNG or GIF, up to 10 MB and 16 megapixels.
Videos: MP4 or WebM, up to 50 MB. Browser codec support still applies.
Uploaded files stay in `.local/uploads`; MySQL stores their post/media references.
They survive sign-out and restarts. Keep `.local` when updating this copy.
Each laptop has its own database and uploads; GitHub shares code, not this data.
No upload service, extra frontend dependency or schema change is required.

The plain-English demonstration guide is `../docs/SIMPLE GUIDE.txt`.
Upload checks: `python scripts/test_uploads.py` uses two local sample videos
in `.local/test-media/flower.mp4` and `flower.webm` (test fixtures, not packaged).
Helper checks: `node --test frontend/tests/requests.test.mjs`.
Some original seed videos point to placeholder media.careerup.io URLs and were
never supplied as files. They are labeled Demo video not included. Original
seed records remain intact; use your own uploaded video for the demonstration.
