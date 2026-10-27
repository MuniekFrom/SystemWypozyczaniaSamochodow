export function bindPasswordToggles() {
    document.querySelectorAll("[data-password-target]").forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordTarget);

            if (!input) {
                return;
            }

            const shouldShowPassword = input.type === "password";
            input.type = shouldShowPassword ? "text" : "password";
            button.textContent = shouldShowPassword ? "Ukryj" : "Pokaż";
            button.setAttribute(
                "aria-label",
                shouldShowPassword ? "Ukryj hasło" : "Pokaż hasło"
            );
        });
    });
}

export function setAuthStatus(element, message = "", type = "") {
    if (!element) {
        return;
    }

    element.textContent = message;
    element.classList.remove("auth-status--error", "auth-status--success");

    if (type) {
        element.classList.add(`auth-status--${type}`);
    }
}

export function setSubmitLoading(button, isLoading, labels) {
    if (!button) {
        return;
    }

    button.disabled = isLoading;
    button.textContent = isLoading ? labels.loading : labels.idle;
}

export function translateAuthError(error, context) {
    const message = String(error?.message ?? "").toLowerCase();

    if (message.includes("invalid email or password")) {
        return "Nieprawidłowy adres e-mail lub hasło.";
    }

    if (message.includes("account is blocked")) {
        return "To konto zostało zablokowane. Skontaktuj się z administratorem.";
    }

    if (message.includes("already exists")) {
        return "Konto z tym adresem e-mail już istnieje.";
    }

    if (message.includes("failed to fetch") || message.includes("network")) {
        return "Nie udało się połączyć z serwerem. Spróbuj ponownie.";
    }

    return context === "register"
        ? "Nie udało się utworzyć konta. Sprawdź dane i spróbuj ponownie."
        : "Nie udało się zalogować. Spróbuj ponownie.";
}
