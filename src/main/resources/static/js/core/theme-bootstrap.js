(function applySavedThemeBeforeFirstPaint() {
    const storageKey = "car-rental-theme";
    let savedTheme = null;

    try {
        savedTheme = window.localStorage.getItem(storageKey);
    } catch {
        savedTheme = null;
    }

    const theme = savedTheme === "light" || savedTheme === "dark"
        ? savedTheme
        : window.matchMedia("(prefers-color-scheme: dark)").matches
            ? "dark"
            : "light";

    document.documentElement.dataset.theme = theme;
})();
