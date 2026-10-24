const API_BASE_URL = "/api";

export async function apiRequest(path, options = {}) {
    const headers = new Headers(options.headers);

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(
        `${API_BASE_URL}${path}`,
        {
            ...options,
            headers
        }
    );

    if (response.status === 204) {
        return null;
    }

    const contentType =
        response.headers.get("content-type") ?? "";

    const responseBody = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const message =
            responseBody?.message
            ?? "Nie udało się wykonać żądania.";

        throw new Error(message);
    }

    return responseBody;
}

export function getCars() {
    return apiRequest("/cars");
}

export function getAvailableCars(startDate, endDate) {
    const query = new URLSearchParams({
        startDate,
        endDate
    });

    return apiRequest(
        `/cars/available?${query.toString()}`
    );
}