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
