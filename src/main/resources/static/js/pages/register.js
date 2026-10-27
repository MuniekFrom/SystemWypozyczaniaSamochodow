import { apiRequest } from "/js/core/api.js";
import {
    bindPasswordToggles,
    setAuthStatus,
    setSubmitLoading,
    translateAuthError
} from "/js/components/auth-form.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { initPageLoader } from "/js/core/page-loader.js";

const form = document.querySelector("#register-form");
const firstNameInput = document.querySelector("#register-first-name");
const lastNameInput = document.querySelector("#register-last-name");
const emailInput = document.querySelector("#register-email");
const phoneInput = document.querySelector("#register-phone");
const passwordInput = document.querySelector("#register-password");
const confirmPasswordInput = document.querySelector("#register-confirm-password");
const submitButton = document.querySelector("#register-submit");
const status = document.querySelector("#register-status");

renderSiteHeader();
initPageLoader();
bindPasswordToggles();
bindRegistrationEvents();

function bindRegistrationEvents() {
    form?.addEventListener("submit", handleRegistration);

    confirmPasswordInput?.addEventListener("input", () => {
        confirmPasswordInput.setCustomValidity("");
    });
}

async function handleRegistration(event) {
    event.preventDefault();
    setAuthStatus(status);

    if (passwordInput.value !== confirmPasswordInput.value) {
        confirmPasswordInput.setCustomValidity("Hasła muszą być identyczne.");
        confirmPasswordInput.reportValidity();
        return;
    }

    confirmPasswordInput.setCustomValidity("");

    if (!form?.reportValidity()) {
        return;
    }

    setSubmitLoading(submitButton, true, {
        idle: "Zarejestruj się",
        loading: "Tworzenie konta..."
    });

    try {
        await apiRequest("/users", {
            method: "POST",
            body: JSON.stringify({
                firstName: firstNameInput.value.trim(),
                lastName: lastNameInput.value.trim(),
                email: emailInput.value.trim(),
                password: passwordInput.value,
                phoneNumber: phoneInput.value.trim()
            })
        });

        setAuthStatus(
            status,
            "Konto zostało utworzone. Za chwilę przejdziesz do logowania...",
            "success"
        );

        const query = new URLSearchParams({
            registered: "true",
            email: emailInput.value.trim()
        });

        window.setTimeout(() => {
            window.location.assign(`/html/login.html?${query.toString()}`);
        }, 900);
    } catch (error) {
        setAuthStatus(status, translateAuthError(error, "register"), "error");
    } finally {
        setSubmitLoading(submitButton, false, {
            idle: "Zarejestruj się",
            loading: "Tworzenie konta..."
        });
    }
}
