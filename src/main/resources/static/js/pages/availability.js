import { getAvailableCars } from "/js/core/api.js";
import { renderCarCards } from "/js/components/car-card.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { initPageLoader } from "/js/core/page-loader.js";

const TIME_SLOT_MINUTES = 30;

const form = document.querySelector("#availability-form");
const startDateInput = document.querySelector("#start-date");
const startTimeInput = document.querySelector("#start-time");
const endDateInput = document.querySelector("#end-date");
const endTimeInput = document.querySelector("#end-time");
const submitButton = document.querySelector("#availability-submit");
const status = document.querySelector("#availability-status");
const container = document.querySelector("#available-cars");

renderSiteHeader("availability");
initPageLoader();
initializeAvailabilityPage();

function initializeAvailabilityPage() {
    if (
        !form
        || !startDateInput
        || !startTimeInput
        || !endDateInput
        || !endTimeInput
    ) {
        return;
    }

    populateTimeSelect(startTimeInput);
    populateTimeSelect(endTimeInput);

    const query = new URLSearchParams(window.location.search);
    const requestedStart = splitDateTime(query.get("startAt"));
    const requestedEnd = splitDateTime(query.get("endAt"));
    const firstPossibleStart = roundUpToTimeSlot(new Date());
    const suggestedEnd = new Date(firstPossibleStart);
    suggestedEnd.setHours(suggestedEnd.getHours() + 1);
    const today = formatDate(new Date());

    startDateInput.min = today;
    endDateInput.min = today;

    startDateInput.value = requestedStart?.date ?? formatDate(firstPossibleStart);
    startTimeInput.value = requestedStart?.time ?? formatTime(firstPossibleStart);
    endDateInput.value = requestedEnd?.date ?? formatDate(suggestedEnd);
    endTimeInput.value = requestedEnd?.time ?? formatTime(suggestedEnd);

    updateDateAndTimeLimits();

    startDateInput.addEventListener("change", handleStartChange);
    startTimeInput.addEventListener("change", handleStartChange);
    endDateInput.addEventListener("change", handleEndChange);
    endTimeInput.addEventListener("change", resetResults);
    form.addEventListener("submit", handleSubmit);

    const period = getSelectedPeriod();
    if (requestedStart && requestedEnd && period && period.endAt > period.startAt) {
        searchAvailableCars(period.startAt, period.endAt);
    }
}

function handleStartChange() {
    updateDateAndTimeLimits();
    resetResults();
}

function handleEndChange() {
    updateEndTimeOptions();
    resetResults();
}

function updateDateAndTimeLimits() {
    if (!startDateInput || !startTimeInput || !endDateInput || !endTimeInput) {
        return;
    }

    const today = formatDate(new Date());
    disablePastStartTimes(startDateInput.value === today);

    endDateInput.min = startDateInput.value || today;

    if (!endDateInput.value || endDateInput.value < endDateInput.min) {
        endDateInput.value = endDateInput.min;
    }

    updateEndTimeOptions();
}

function disablePastStartTimes(isToday) {
    if (!startTimeInput) {
        return;
    }

    const minimumTime = formatTime(roundUpToTimeSlot(new Date()));

    Array.from(startTimeInput.options).forEach(option => {
        option.disabled = isToday && option.value < minimumTime;
    });

    if (startTimeInput.selectedOptions[0]?.disabled) {
        selectFirstEnabledOption(startTimeInput);
    }
}

function updateEndTimeOptions() {
    if (!startDateInput || !startTimeInput || !endDateInput || !endTimeInput) {
        return;
    }

    const sameDay = startDateInput.value === endDateInput.value;

    Array.from(endTimeInput.options).forEach(option => {
        option.disabled = sameDay && option.value <= startTimeInput.value;
    });

    if (endTimeInput.selectedOptions[0]?.disabled) {
        selectFirstEnabledOption(endTimeInput);
    }
}

async function handleSubmit(event) {
    event.preventDefault();

    const period = getSelectedPeriod();

    if (!period) {
        setError("Wybierz datę i godzinę odbioru oraz zwrotu.");
        return;
    }

    if (period.endAt <= period.startAt) {
        setError("Termin zwrotu musi przypadać po terminie odbioru.");
        return;
    }

    await searchAvailableCars(period.startAt, period.endAt);
}

async function searchAvailableCars(startAt, endAt) {
    setLoadingState(true);
    setStatus("Sprawdzanie dostępności samochodów...");

    try {
        const cars = await getAvailableCars(startAt, endAt);

        renderCarCards(container, cars, { startAt, endAt });

        const query = new URLSearchParams({ startAt, endAt });
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

function getSelectedPeriod() {
    const startAt = combineDateAndTime(
        startDateInput?.value,
        startTimeInput?.value
    );
    const endAt = combineDateAndTime(
        endDateInput?.value,
        endTimeInput?.value
    );

    return startAt && endAt ? { startAt, endAt } : null;
}

function populateTimeSelect(select) {
    select.replaceChildren();

    for (let minutes = 0; minutes < 24 * 60; minutes += TIME_SLOT_MINUTES) {
        const time = formatMinutes(minutes);
        select.add(new Option(time, time));
    }
}

function selectFirstEnabledOption(select) {
    const option = Array.from(select.options).find(item => !item.disabled);
    select.value = option?.value ?? "";
}

function splitDateTime(value) {
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value ?? "")) {
        return null;
    }

    const [date, time] = value.split("T");
    return { date, time };
}

function combineDateAndTime(date, time) {
    return date && time ? `${date}T${time}` : null;
}

function roundUpToTimeSlot(date) {
    const rounded = new Date(date);
    rounded.setSeconds(0, 0);

    const remainder = rounded.getMinutes() % TIME_SLOT_MINUTES;
    if (remainder !== 0) {
        rounded.setMinutes(rounded.getMinutes() + TIME_SLOT_MINUTES - remainder);
    }

    return rounded;
}

function formatMinutes(totalMinutes) {
    const hours = String(Math.floor(totalMinutes / 60)).padStart(2, "0");
    const minutes = String(totalMinutes % 60).padStart(2, "0");
    return `${hours}:${minutes}`;
}

function formatTime(date) {
    return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
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

function resetResults() {
    container?.replaceChildren();
    setStatus("Wybierz termin i wyszukaj dostępne samochody.");
}
