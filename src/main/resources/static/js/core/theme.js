const THEME_STORAGE_KEY = "car-rental-theme";

export function initThemeSettings() {
    const root = document.documentElement;

    const settingsToggle =
        document.querySelector("#settings-toggle");

    const themeMenu =
        document.querySelector("#theme-menu");

    const themeOptions =
        document.querySelectorAll("[data-theme-value]");

    const systemPrefersDark =
        window.matchMedia("(prefers-color-scheme: dark)");

    function getSavedTheme() {
        const savedTheme =
            localStorage.getItem(THEME_STORAGE_KEY);

        if (savedTheme === "light" || savedTheme === "dark") {
            return savedTheme;
        }

        return null;
    }

    function updateSelectedOption(theme) {
        themeOptions.forEach(option => {
            const isSelected =
                option.dataset.themeValue === theme;

            option.classList.toggle(
                "is-active",
                isSelected
            );

            option.setAttribute(
                "aria-checked",
                String(isSelected)
            );
        });
    }

    function setTheme(theme, saveTheme = true) {
        root.dataset.theme = theme;

        updateSelectedOption(theme);

        if (saveTheme) {
            localStorage.setItem(
                THEME_STORAGE_KEY,
                theme
            );
        }
    }

    const initialTheme =
        getSavedTheme()
        ?? (systemPrefersDark.matches ? "dark" : "light");

    setTheme(initialTheme, false);

    if (!settingsToggle || !themeMenu) {
        return;
    }

    function openMenu() {
        themeMenu.hidden = false;

        settingsToggle.setAttribute(
            "aria-expanded",
            "true"
        );
    }

    function closeMenu() {
        themeMenu.hidden = true;

        settingsToggle.setAttribute(
            "aria-expanded",
            "false"
        );
    }

    settingsToggle.addEventListener("click", event => {
        event.stopPropagation();

        if (themeMenu.hidden) {
            openMenu();
        } else {
            closeMenu();
        }
    });

    themeOptions.forEach(option => {
        option.addEventListener("click", () => {
            setTheme(option.dataset.themeValue);
            closeMenu();
        });
    });

    document.addEventListener("click", event => {
        if (!event.target.closest(".theme-settings")) {
            closeMenu();
        }
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeMenu();
            settingsToggle.focus();
        }
    });
}