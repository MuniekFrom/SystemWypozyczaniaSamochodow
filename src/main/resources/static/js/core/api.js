import { getAccessToken } from "/js/core/auth.js";

const API_BASE_URL = "/api";

export async function apiRequest(path, options = {}) {
    const headers = new Headers(options.headers);
    const accessToken = getAccessToken();

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    if (accessToken && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${accessToken}`);
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers
    });

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") ?? "";
    const responseBody = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const message = responseBody?.message
            ?? (typeof responseBody === "string" && responseBody)
            ?? "Nie udało się wykonać żądania.";

        const error = new Error(message);
        error.status = response.status;
        throw error;
    }

    return responseBody;
}

export function getCars() {
    return apiRequest("/cars");
}

export function getCarById(carId) {
    return apiRequest(`/cars/${carId}`);
}

export function getAvailableCars(startDate, endDate) {
    const query = new URLSearchParams({ startDate, endDate });
    return apiRequest(`/cars/available?${query.toString()}`);
}

export function getCurrentUser() {
    return apiRequest("/users/me");
}

export function getMyReservations() {
    return apiRequest("/reservations/me");
}

export function cancelReservation(reservationId) {
    return apiRequest(`/reservations/${reservationId}/cancel`, {
        method: "PATCH"
    });
}
