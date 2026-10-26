import { getAvailableCars } from "/js/core/api.js";
import { renderCarCards } from "/js/components/car-card.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { initPageLoader } from "/js/core/page-loader.js";

const form = document.querySelector("#availability-form");
const startDateInput = document.querySelector("#start-date");
const endDateInput = document.querySelector("#end-date");
const submitButton = document.querySelector("#availability-submit");
const status = document.querySelector("#availability-status");
const container = document.querySelector("#available-cars");

renderSiteHeader("availability");
initPageLoader();
initializeAvailabilityPage();

function initializeAvailabilityPage() {
    if (!form || !startDateInput || !endDateInput) {
        return;
    }

    const query = new URLSearchParams(window.location.search);
    const startDate = query.get("startDate") ?? "";
    const endDate = query.get("endDate") ?? "";
    const today = formatDate(new Date());

    startDateInput.min = today;
    endDateInput.min = today;
    startDateInput.value = startDate;
    endDateInput.value = endDate;

    updateEndDateMinimum();

    startDateInput.addEventListener("change", updateEndDateMinimum);
    form.addEventListener("submit", handleSubmit);

    if (startDate && endDate && endDate > startDate) {
        searchAvailableCars(startDate, endDate);
    }
}

function updateEndDateMinimum() {
    if (!startDateInput?.value || !endDateInput) {
        return;
    }

    const minimumEndDate = addDays(startDateInput.value, 1);
    endDateInput.min = minimumEndDate;

    if (endDateInput.value && endDateInput.value < minimumEndDate) {
        endDateInput.value = minimumEndDate;
    }
}

async function handleSubmit(event) {
    event.preventDefault();

    const startDate = startDateInput?.value ?? "";
    const endDate = endDateInput?.value ?? "";

    if (!startDate || !endDate) {
        setError("Wybierz datę odbioru i zwrotu.");
        return;
    }

    if (endDate <= startDate) {
        setError("Data zwrotu musi przypadać po dacie odbioru.");
        return;
    }

    await searchAvailableCars(startDate, endDate);
}

async function searchAvailableCars(startDate, endDate) {
    setLoadingState(true);
    setStatus("Sprawdzanie dostępności samochodów...");

    try {
        const cars = await getAvailableCars(startDate, endDate);

        renderCarCards(container, cars, {
            startDate,
            endDate
        });

        const query = new URLSearchParams({ startDate, endDate });
        window.history.replaceState(null, "", `?${query.toString()}`);

        setStatus(
            cars.length > 0
                ? `Dostępne samochody: ${cars.length}.`
                : "Brak dostępnych samochodów w wybranym terminie."
        );

        document.querySelector("#results-title")?.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    } catch (error) {
        setError(error.message);
    } finally {
        setLoadingState(false);
    }
}

function setLoadingState(isLoading) {
    if (!submitButton) {
        return;
    }

    submitButton.disabled = isLoading;
    submitButton.textContent = isLoading
        ? "Sprawdzanie..."
        : "Szukaj samochodów";
}

function setStatus(message) {
    if (!status) {
        return;
    }

    status.textContent = message;
    status.classList.remove("availability-results__status--error");
}

function setError(message) {
    if (!status) {
        return;
    }

    status.textContent = message;
    status.classList.add("availability-results__status--error");
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function addDays(dateValue, days) {
    const date = new Date(`${dateValue}T12:00:00`);
    date.setDate(date.getDate() + days);

    return formatDate(date);
}
