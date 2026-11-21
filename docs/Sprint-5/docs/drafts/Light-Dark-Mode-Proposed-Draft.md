# Light and Dark Mode Proposed Draft

**Status: proposed for team review.** The accompanying application ZIP includes
the working local version. Nothing has been pushed to GitHub. This Markdown
file is supplied with the download, outside the repository.

## What it does

- Dark stays the default. The top-right button switches to Light mode or back.
- The choice is remembered in this browser for this application address.
- Switching does not reload the page, clear a form, sign anyone out or restart media.
- No database change, new library, polling or theme download is needed.
- The original splash artwork and project media keep their original colors.

## Proposed light colors

The direction is a TikTok-inspired neutral interface, with CareerUp's existing
purple branding retained. These are our proposed CareerUp colors, not a claim
that TikTok publishes or uses this exact palette.

| Part | Light color |
| --- | --- |
| Page | `#f7f7fa` |
| Cards and navigation | `#ffffff` |
| Main text | `#161823` |
| Supporting text | `#595963` |
| Borders | `#dcdce4` |
| Links and active labels | `#5b21b6` |
| Soft purple highlights | `#f0eafa` |

The sign-in hero remains dark in both modes to preserve the original brand
panel. Light mode applies to the sign-in form and all five role workspaces,
including their forms, tables, menus, feedback messages and profile dialogs.

## Code

### Browser preference and first paint

File: `frontend/public/theme.js`. Load this before React so the saved theme is
applied before the page appears. Restricted storage falls back safely to dark;
switching still works for the current page.

```javascript
// APPEARANCE: runs before the page paints; no request, library or database change.
(function (browser) {
  "use strict";
  const key = "careerup.theme";
  const normalize = value => value === "light" ? "light" : "dark";
  function read() {
    try { return normalize(browser.localStorage.getItem(key)); }
    catch { return "dark"; } // Private/restricted browsers still work.
  }
  function apply(value, remember = true) {
    const theme = normalize(value);
    browser.document.documentElement.dataset.theme = theme;
    const meta = browser.document.querySelector('meta[name="theme-color"]');
    if (meta) meta.content = theme === "light" ? "#f7f7fa" : "#0f0f1a";
    if (remember) {
      try { browser.localStorage.setItem(key, theme); } catch { /* Session-only choice. */ }
    }
    browser.dispatchEvent(new browser.Event("careerup:theme"));
  }
  browser.CareerUpTheme = {
    get: () => normalize(browser.document.documentElement.dataset.theme),
    toggle: () => apply(browser.CareerUpTheme.get() === "dark" ? "light" : "dark"),
  };
  apply(read(), false);
  // Keep two CareerUp tabs on the same origin in sync; no polling or reloading.
  browser.addEventListener("storage", event => {
    if (event.key === key || event.key === null) apply(read(), false);
  });
})(window);
```

### Small React switch

File: `frontend/src/ThemeToggle.jsx`. The visible label says which mode the next
click will select. Icons already come from the project's existing library.

```jsx
import React, {useEffect, useState} from "react";
import {Sun, Moon} from "lucide-react";

// THEME SWITCH: changes colors in place; forms, media and sign-in stay mounted.
export default function ThemeToggle() {
  const [theme,setTheme]=useState(()=>window.CareerUpTheme?.get()||"dark");
  useEffect(()=>{
    const update=()=>setTheme(window.CareerUpTheme?.get()||"dark");
    window.addEventListener("careerup:theme",update);
    update();
    return()=>window.removeEventListener("careerup:theme",update);
  },[]);
  const next=theme==="dark"?"Light":"Dark";
  return <button type="button" className="theme-toggle"
    title={`Switch to ${next.toLowerCase()} mode`}
    aria-label={`Switch to ${next.toLowerCase()} mode`}
    onClick={()=>window.CareerUpTheme?.toggle()}>
    {theme==="dark"?<Sun size={17} aria-hidden="true"/>:<Moon size={17} aria-hidden="true"/>}
    <span>{next} mode</span>
  </button>;
}
```

### Light palette and control styles

Append to `frontend/src/styles.css`. Existing dark settings remain in place.
No image/video inversion or animation is used.

```css
/* 10. LIGHT / DARK APPEARANCE — dark defaults above stay unchanged.
   Light uses neutral surfaces and near-black text, retaining CareerUp's purple brand.
   Actual photos/videos and the original dark splash artwork are never inverted. */
:root { --text: #ffffff; color: var(--text); color-scheme: dark; }
:root[data-theme="light"] {
  --bg: #f7f7fa;
  --sidebar: #ffffff;
  --card: #ffffff;
  --border: #dcdce4;
  --muted: #595963;
  --text: #161823;
  --purple: #7e22ce;
  color: var(--text);
  background: var(--bg);
  color-scheme: light;
}
.theme-toggle { min-height: 36px; white-space: nowrap; font-size: 11px; padding: 8px 10px; }
.login-theme-switch { position: absolute; top: 18px; right: 24px; }
:root[data-theme="light"] :is(input,select,textarea,.action-form input,.action-form select,.action-form textarea) {
  color: var(--text); background: #ffffff; border-color: #8c8c97;
}
:root[data-theme="light"] input::placeholder { color: #686873; }
:root[data-theme="light"] :is(.demo-roles button,.filters,.table-wrap,.profile-dialog,.topbar) { background: #ffffff; color: var(--text); border-color: var(--border); }
:root[data-theme="light"] :is(.search-form,th,.close) { background: #f0f0f4; color: var(--text); }
:root[data-theme="light"] .search-form input { background: transparent; }
:root[data-theme="light"] :is(.breadcrumb strong,.mini-brand strong,.table-user strong,.empty h3,.saved-post-notice button) { color: var(--text); }
:root[data-theme="light"] :is(td,.app-footer) { color: var(--text); border-color: var(--border); }
:root[data-theme="light"] :is(.workspace-label,.later-note,.signout,.breadcrumb,.breadcrumb span,.app-footer span,.login-main footer,.nav-item) { color: var(--muted); }
:root[data-theme="light"] :is(.role-pill,.switch-account,.profile-button,.search-form button,.count,.badge,.tags span,.nav-item.active) {
  color: #5b21b6; background: #f0eafa; border-color: #d8cbf3;
}
:root[data-theme="light"] :is(.eyebrow,.major-name,.class-year,.request-card summary,.reset,.demo code,.empty,.loading,.no-access,.heading-symbol,.nav-item.active > svg) { color: #5b21b6; }
:root[data-theme="light"] .media-fallback { color: #5b21b6; background: #f0eafa; }
:root[data-theme="light"] .status { color: #854d0e; background: #fff7df; border-color: #e8d49c; }
:root[data-theme="light"] .status.positive { color: #166534; background: #eaf7ee; border-color: #b8ddc4; }
:root[data-theme="light"] .action-success { color: #166534; background: #eaf7ee; border-color: #b8ddc4; }
:root[data-theme="light"] :is(.action-error,.error) { color: #9f1239; background: #fff0f3; border-color: #edbdc8; }
:root[data-theme="light"] .modal-shade { background: #16182388; }
/* The splash hero remains the authoritative dark brand panel in both modes. */
:root[data-theme="light"] .login-story { color: #ffffff; --muted: #bebdcc; color-scheme: dark; }
:root[data-theme="light"] .login-story .eyebrow { color: #b9a4fb; }
@media(max-width:480px) {
  .account-actions .theme-toggle span { display: none; }
  .account-actions .theme-toggle { padding: 8px; }
}
.session-notice { color: var(--muted); font-size: 14px; line-height: 1.6; }
```

### Connect the switch

In `frontend/index.html`, put this script in the head before the React entry:

```html
<script src="/theme.js"></script>
```

In `frontend/src/main.jsx`, import the component and use it in the existing
sign-in header area and signed-in top bar:

```jsx
import ThemeToggle from "./ThemeToggle.jsx";

// Sign-in form area:
<div className="login-theme-switch"><ThemeToggle /></div>

// Signed-in account-actions area:
<ThemeToggle />
```

## Try it

1. Launch the included application normally; no rebuild is needed for the ZIP.
2. Click Light mode at the top right, then Dark mode to switch back.
3. Enter part of a form and switch modes. Your entry should remain.
4. Refresh at the same address. The selected mode should return.
5. Check each role, the advisor profile dialog and a picture/video preview.

## Checks and remaining review

Seventeen Node test groups passed, including theme preferences, storage
fallback, cross-tab updates, startup order and light text-pair contrast.
The tested normal text pairs meet at least 4.5:1 contrast; this is not a claim
that the entire interface has passed an accessibility audit.
Production builds and live application/database results are described in the
existing project verification notes. Browser visual review, real playback and
teammate/Mac checks still need to be done on the actual machines.

## Presenter wording

> This is an optional appearance setting. Dark is our original CareerUp design.
> Light mode changes the interface colors while keeping our branding and media
> intact. It does not change the five required role workflows or saved data.
