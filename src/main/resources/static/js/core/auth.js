const ACCESS_TOKEN_KEY = "car-rental-access-token";
const TOKEN_TYPE_KEY = "car-rental-token-type";
const TOKEN_EXPIRATION_KEY = "car-rental-token-expiration";

export function saveAuthSession(loginResponse, persistent = true) {
    clearAuthSession();

    const storage = persistent ? window.localStorage : window.sessionStorage;
    const expiresAt = Date.now() + Number(loginResponse.expiresIn) * 1000;

    storage.setItem(ACCESS_TOKEN_KEY, loginResponse.accessToken);
    storage.setItem(TOKEN_TYPE_KEY, loginResponse.tokenType ?? "Bearer");
    storage.setItem(TOKEN_EXPIRATION_KEY, String(expiresAt));
}

export function getAccessToken() {
    const sessionToken = readValidToken(window.sessionStorage);

    if (sessionToken) {
        return sessionToken;
    }

    return readValidToken(window.localStorage);
}

export function clearAuthSession() {
    [window.localStorage, window.sessionStorage].forEach(storage => {
        storage.removeItem(ACCESS_TOKEN_KEY);
        storage.removeItem(TOKEN_TYPE_KEY);
        storage.removeItem(TOKEN_EXPIRATION_KEY);
    });
}

export function getRoleFromToken(accessToken) {
    const payload = decodeJwtPayload(accessToken);
    return payload?.role ?? null;
}

export function getDashboardUrl(role) {
    switch (role) {
        case "ADMIN":
            return "/html/admin.html";
        case "EMPLOYEE":
            return "/html/employee.html";
        case "CLIENT":
            return "/html/client.html";
        default:
            return "/";
    }
}

function readValidToken(storage) {
    const accessToken = storage.getItem(ACCESS_TOKEN_KEY);
    const expiresAt = Number(storage.getItem(TOKEN_EXPIRATION_KEY));

    if (!accessToken) {
        return null;
    }

    if (Number.isFinite(expiresAt) && expiresAt > 0 && Date.now() >= expiresAt) {
        storage.removeItem(ACCESS_TOKEN_KEY);
        storage.removeItem(TOKEN_TYPE_KEY);
        storage.removeItem(TOKEN_EXPIRATION_KEY);
        return null;
    }

    return accessToken;
}

function decodeJwtPayload(accessToken) {
    try {
        const encodedPayload = accessToken.split(".")[1];

        if (!encodedPayload) {
            return null;
        }

        const base64 = encodedPayload
            .replaceAll("-", "+")
            .replaceAll("_", "/")
            .padEnd(Math.ceil(encodedPayload.length / 4) * 4, "=");

        const decodedPayload = decodeURIComponent(
            window.atob(base64)
                .split("")
                .map(character => `%${character.charCodeAt(0).toString(16).padStart(2, "0")}`)
                .join("")
        );

        return JSON.parse(decodedPayload);
    } catch {
        return null;
    }
}
