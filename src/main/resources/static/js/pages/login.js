import { apiRequest } from "/js/core/api.js";
import {
    getDashboardUrl,
    getRoleFromToken,
    saveAuthSession
} from "/js/core/auth.js";
import {
    bindPasswordToggles,
    setAuthStatus,
    setSubmitLoading,
    translateAuthError
} from "/js/components/auth-form.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { initPageLoader } from "/js/core/page-loader.js";

const form = document.querySelector("#login-form");
const emailInput = document.querySelector("#login-email");
const passwordInput = document.querySelector("#login-password");
const rememberMeInput = document.querySelector("#remember-me");
const submitButton = document.querySelector("#login-submit");
const status = document.querySelector("#login-status");

renderSiteHeader();
initPageLoader();
bindPasswordToggles();
initializeLoginPage();

function initializeLoginPage() {
    const query = new URLSearchParams(window.location.search);
    const registeredEmail = query.get("email");

    if (registeredEmail && emailInput) {
        emailInput.value = registeredEmail;
    }

    if (query.get("registered") === "true") {
        setAuthStatus(
            status,
            "Konto zostało utworzone. Możesz się teraz zalogować.",
            "success"
        );
    }

    form?.addEventListener("submit", handleLogin);
}

async function handleLogin(event) {
    event.preventDefault();

    if (!form?.reportValidity()) {
        return;
    }

    setAuthStatus(status);
    setSubmitLoading(submitButton, true, {
        idle: "Zaloguj się",
        loading: "Logowanie..."
    });

    try {
        const response = await apiRequest("/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: emailInput.value.trim(),
                password: passwordInput.value
            })
        });

        saveAuthSession(response, rememberMeInput?.checked ?? false);

        setAuthStatus(status, "Logowanie zakończone. Przenosimy Cię do konta...", "success");

        const role = getRoleFromToken(
            response.accessToken
        );

        const redirectUrl =
            getRedirectUrlAfterLogin(role);

        window.setTimeout(() => {
            window.location.assign(redirectUrl);
        }, 500);
    } catch (error) {
        setAuthStatus(status, translateAuthError(error, "login"), "error");
        passwordInput?.focus();
    } finally {
        setSubmitLoading(submitButton, false, {
            idle: "Zaloguj się",
            loading: "Logowanie..."
        });
    }
}

function getRedirectUrlAfterLogin(role) {
    const defaultUrl =
        getDashboardUrl(role);

    if (role !== "CLIENT") {
        return defaultUrl;
    }

    const query =
        new URLSearchParams(
            window.location.search
        );

    const requestedRedirect =
        query.get("redirect");

    if (!requestedRedirect) {
        return defaultUrl;
    }

    try {
        const redirectUrl =
            new URL(
                requestedRedirect,
                window.location.origin
            );

        if (
            redirectUrl.origin
            !== window.location.origin
        ) {
            return defaultUrl;
        }

        return (
            redirectUrl.pathname
            + redirectUrl.search
            + redirectUrl.hash
        );
    } catch {
        return defaultUrl;
    }
}
