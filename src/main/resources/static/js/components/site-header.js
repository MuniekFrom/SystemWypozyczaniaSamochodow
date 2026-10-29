import { initThemeSettings } from "/js/core/theme.js";
import {
    clearAuthSession,
    getAccessToken,
    getDashboardUrl,
    getRoleFromToken
} from "/js/core/auth.js";

const MOBILE_NAVIGATION_BREAKPOINT = 1080;

const navigationItems = [
    {
        key: "fleet",
        label: "Flota",
        href: "/html/fleet.html"
    },
    {
        key: "availability",
        label: "Sprawdź dostępność",
        href: "/html/availability.html"
    },
    {
        key: "terms",
        label: "Warunki wynajmu",
        href: "/html/rental-terms.html"
    }
];

export function renderSiteHeader(activePage = "") {
    const header = document.querySelector("#site-header");

    if (!header) {
        return;
    }

    const currentPath = window.location.pathname;
    const isLoginPage = currentPath.endsWith("/login.html");
    const isRegisterPage = currentPath.endsWith("/register.html");
    const accessToken = getAccessToken();
    const role = accessToken ? getRoleFromToken(accessToken) : null;

    header.className = "site-header";
    header.innerHTML = `
        <div class="container site-header__content">
            <a class="brand" href="/" aria-label="Car Rental – strona główna">
                <img
                    class="brand__logo"
                    src="/images/logo/car-rental-logo-gold.webp"
                    alt="Car Rental"
                    width="2171"
                    height="724"
                >
            </a>

            <div id="site-header-panel" class="site-header__panel">
                <nav class="main-navigation" aria-label="Główna nawigacja">
                    ${navigationItems.map(item => createNavigationLink(item, activePage)).join("")}
                </nav>

                <div class="site-header__actions">
                    ${createThemeSettings()}
                    ${createAccountActions({
        accessToken,
        role,
        activePage,
        isLoginPage,
        isRegisterPage
    })}
                </div>
            </div>

            <button
                id="navigation-toggle"
                class="navigation-toggle"
                type="button"
                aria-label="Otwórz menu"
                aria-expanded="false"
                aria-controls="site-header-panel"
            >
                <span class="navigation-toggle__icon" aria-hidden="true">
                    <span></span>
                </span>
            </button>
        </div>
    `;

    initThemeSettings();
    bindLogoutButton();
    bindNavigationMenu(header);
}

function createNavigationLink(item, activePage) {
    const isActive = item.key === activePage;

    return `
        <a
            class="main-navigation__link${isActive ? " is-active" : ""}"
            href="${item.href}"
            ${isActive ? 'aria-current="page"' : ""}
        >
            ${item.label}
        </a>
    `;
}

function createThemeSettings() {
    return `
        <div class="theme-settings">
            <button
                id="settings-toggle"
                class="settings-toggle"
                type="button"
                aria-label="Ustawienia wyglądu"
                aria-expanded="false"
                aria-controls="theme-menu"
            >
                <svg aria-hidden="true" viewBox="0 0 24 24" width="20" height="20">
                    <path
                        d="M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.8"
                    ></path>
                    <path
                        d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.12 2.12-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1 1.55V20.3h-3v-.09a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06-2.12-2.12.06-.06A1.7 1.7 0 0 0 7.08 15a1.7 1.7 0 0 0-1.55-1H5.4v-3h.13a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.12-2.12.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1-1.55V4.7h3v.09a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.12 2.12-.06.06A1.7 1.7 0 0 0 19.4 10a1.7 1.7 0 0 0 1.55 1h.13v3h-.13a1.7 1.7 0 0 0-1.55 1Z"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    ></path>
                </svg>
            </button>

            <div id="theme-menu" class="theme-menu" role="menu" hidden>
                <p class="theme-menu__title" role="presentation">Wygląd strony</p>

                <button
                    class="theme-option"
                    type="button"
                    data-theme-value="light"
                    role="menuitemradio"
                    aria-checked="false"
                >
                    <span class="theme-option__icon" aria-hidden="true">☀</span>
                    Tryb dzienny
                </button>

                <button
                    class="theme-option"
                    type="button"
                    data-theme-value="dark"
                    role="menuitemradio"
                    aria-checked="false"
                >
                    <span class="theme-option__icon" aria-hidden="true">☾</span>
                    Tryb nocny
                </button>
            </div>
        </div>
    `;
}

function createAccountActions({
                                  accessToken,
                                  role,
                                  activePage,
                                  isLoginPage,
                                  isRegisterPage
                              }) {
    if (accessToken && role) {
        const dashboardUrl = getDashboardUrl(role);
        const isDashboardActive = activePage === "client"
            || activePage === "employee"
            || activePage === "admin";

        return `
            <a
                class="button button--primary${isDashboardActive ? " is-current" : ""}"
                href="${dashboardUrl}"
                ${isDashboardActive ? 'aria-current="page"' : ""}
            >
                Moje konto
            </a>
            <button id="header-logout" class="button button--outline" type="button">
                Wyloguj
            </button>
        `;
    }

    return `
        <a
            class="button button--outline${isLoginPage ? " is-current" : ""}"
            href="/html/login.html"
            ${isLoginPage ? 'aria-current="page"' : ""}
        >
            Logowanie
        </a>
        <a
            class="button button--primary${isRegisterPage ? " is-current" : ""}"
            href="/html/register.html"
            ${isRegisterPage ? 'aria-current="page"' : ""}
        >
            Rejestracja
        </a>
    `;
}

function bindNavigationMenu(header) {
    const toggle = header.querySelector("#navigation-toggle");
    const panel = header.querySelector("#site-header-panel");

    if (!toggle || !panel) {
        return;
    }

    const setOpen = isOpen => {
        panel.classList.toggle("is-open", isOpen);
        toggle.setAttribute("aria-expanded", String(isOpen));
        toggle.setAttribute("aria-label", isOpen ? "Zamknij menu" : "Otwórz menu");
    };

    toggle.addEventListener("click", event => {
        event.stopPropagation();
        setOpen(!panel.classList.contains("is-open"));
    });

    panel.addEventListener("click", event => {
        if (event.target.closest("a")) {
            setOpen(false);
        }
    });

    document.addEventListener("click", event => {
        if (!header.contains(event.target)) {
            setOpen(false);
        }
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && panel.classList.contains("is-open")) {
            setOpen(false);
            toggle.focus();
        }
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > MOBILE_NAVIGATION_BREAKPOINT) {
            setOpen(false);
        }
    });
}

function bindLogoutButton() {
    document.querySelector("#header-logout")?.addEventListener("click", () => {
        clearAuthSession();
        window.location.assign("/");
    });
}
