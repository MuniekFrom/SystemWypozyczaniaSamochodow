import {
    cancelReservation,
    getCurrentUser,
    getMyReservations
} from "/js/core/api.js";
import {
    clearAuthSession,
    getAccessToken,
    getDashboardUrl,
    getRoleFromToken
} from "/js/core/auth.js";
import { initPageLoader } from "/js/core/page-loader.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { getPrimaryCarImage } from "/js/data/car-images.js";

const ACTIVE_STATUSES = new Set(["PENDING", "CONFIRMED", "IN_PROGRESS"]);
const CANCELLABLE_STATUSES = new Set(["PENDING", "CONFIRMED"]);

const statusLabels = {
    PENDING: "Oczekuje na potwierdzenie",
    CONFIRMED: "Potwierdzona",
    IN_PROGRESS: "W trakcie wynajmu",
    COMPLETED: "Zakończona",
    CANCELLED: "Anulowana"
};

const currencyFormatter = new Intl.NumberFormat("pl-PL", {
    style: "currency",
    currency: "PLN"
});

const dateFormatter = new Intl.DateTimeFormat("pl-PL", {
    day: "2-digit",
    month: "long",
    year: "numeric"
});

const shortDateFormatter = new Intl.DateTimeFormat("pl-PL", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
});

const pageStatus = document.querySelector("#client-page-status");
const reservationsStatus = document.querySelector("#reservations-status");
const reservationsList = document.querySelector("#reservations-list");
const filterButtons = document.querySelectorAll(".reservation-filter");
const cancelDialog = document.querySelector("#cancel-dialog");
const confirmCancellationButton = document.querySelector("#confirm-cancellation");

let reservations = [];
let selectedFilter = "ALL";
let reservationToCancel = null;
let toastTimeoutId = null;

renderSiteHeader("client");
initPageLoader();
protectClientPage();

function protectClientPage() {
    const accessToken = getAccessToken();

    if (!accessToken) {
        redirectToLogin();
        return;
    }

    const role = getRoleFromToken(accessToken);

    if (role && role !== "CLIENT") {
        window.location.replace(getDashboardUrl(role));
        return;
    }

    bindClientEvents();
    loadClientDashboard();
}

function bindClientEvents() {
    filterButtons.forEach(button => {
        button.addEventListener("click", () => {
            selectedFilter = button.dataset.filter ?? "ALL";

            filterButtons.forEach(currentButton => {
                const isActive = currentButton === button;
                currentButton.classList.toggle("is-active", isActive);
                currentButton.setAttribute("aria-pressed", String(isActive));
            });

            renderReservations();
        });
    });

    reservationsList?.addEventListener("click", event => {
        const cancelButton = event.target.closest("[data-cancel-reservation]");

        if (!cancelButton) {
            return;
        }

        const reservationId = Number(cancelButton.dataset.cancelReservation);
        const reservation = reservations.find(item => Number(item.id) === reservationId);

        if (reservation) {
            openCancellationDialog(reservation);
        }
    });

    confirmCancellationButton?.addEventListener("click", confirmCancellation);

    cancelDialog?.addEventListener("close", () => {
        reservationToCancel = null;
        setCancellationLoading(false);
    });
}

async function loadClientDashboard() {
    setPageStatus("Pobieranie danych konta i rezerwacji...");

    try {
        const [user, userReservations] = await Promise.all([
            getCurrentUser(),
            getMyReservations()
        ]);

        reservations = Array.isArray(userReservations) ? userReservations : [];

        renderProfile(user);
        renderStatistics();
        renderNextReservation();
        renderReservations();
        hidePageStatus();
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearAuthSession();
            redirectToLogin();
            return;
        }

        setPageStatus(
            error.message || "Nie udało się pobrać danych panelu klienta.",
            true
        );
        setReservationsStatus("Nie udało się pobrać historii rezerwacji.", true);
    }
}

function renderProfile(user) {
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(" ") || "Klient";
    const initials = [user.firstName, user.lastName]
        .filter(Boolean)
        .map(value => value.trim().charAt(0).toUpperCase())
        .join("")
        .slice(0, 2) || "CR";

    setText("#client-greeting", `Dzień dobry, ${user.firstName || "Kliencie"}`);
    setText("#client-full-name", fullName);
    setText("#client-email", user.email || "—");
    setText("#client-avatar", initials);
    setText("#profile-name", fullName);
    setText("#profile-email", user.email || "—");
    setText("#profile-phone", user.phoneNumber || "Nie podano");
    setText("#profile-created-at", formatDateTime(user.createdAt));
}

function renderStatistics() {
    const activeCount = reservations.filter(reservation =>
        reservation.status === "PENDING" || reservation.status === "CONFIRMED"
    ).length;

    const inProgressCount = reservations.filter(
        reservation => reservation.status === "IN_PROGRESS"
    ).length;

    const completedReservations = reservations.filter(
        reservation => reservation.status === "COMPLETED"
    );

    const completedValue = completedReservations.reduce(
        (sum, reservation) => sum + Number(reservation.totalPrice || 0),
        0
    );

    setText("#active-reservations-count", String(activeCount));
    setText("#in-progress-count", String(inProgressCount));
    setText("#completed-count", String(completedReservations.length));
    setText("#completed-value", currencyFormatter.format(completedValue));
}

function renderNextReservation() {
    const container = document.querySelector("#next-reservation");

    if (!container) {
        return;
    }

    const nextReservation = findNextReservation();

    if (!nextReservation) {
        container.innerHTML = `
            <div class="client-empty-state client-empty-state--compact">
                <span class="client-empty-state__icon" aria-hidden="true">+</span>
                <div>
                    <h3>Nie masz aktywnej rezerwacji</h3>
                    <p>Wybierz termin i zarezerwuj samochód dopasowany do swojej podróży.</p>
                </div>
                <a class="button button--primary" href="/html/availability.html">Znajdź samochód</a>
            </div>
        `;
        return;
    }

    const imagePath = getPrimaryCarImage(nextReservation.carId);
    const carName = `${nextReservation.carBrand} ${nextReservation.carModel}`;
    const countdown = createReservationCountdown(nextReservation);

    container.innerHTML = `
        <div class="next-reservation__media">
            ${imagePath
        ? `<img src="${escapeHtml(imagePath)}" alt="${escapeHtml(carName)}">`
        : '<span class="reservation-image-placeholder" aria-hidden="true">CR</span>'}
            <span class="reservation-status reservation-status--${getStatusClass(nextReservation.status)}">
                ${escapeHtml(getStatusLabel(nextReservation.status))}
            </span>
        </div>

        <div class="next-reservation__body">
            <p class="next-reservation__countdown">${escapeHtml(countdown)}</p>
            <h3>${escapeHtml(carName)}</h3>
            <p class="next-reservation__number">Rezerwacja #${escapeHtml(nextReservation.id)}</p>

            <dl class="next-reservation__details">
                <div>
                    <dt>Odbiór</dt>
                    <dd>${formatDate(nextReservation.startDate)}</dd>
                </div>
                <div>
                    <dt>Zwrot</dt>
                    <dd>${formatDate(nextReservation.endDate)}</dd>
                </div>
                <div>
                    <dt>Łączna cena</dt>
                    <dd>${formatPrice(nextReservation.totalPrice)}</dd>
                </div>
            </dl>

            <div class="next-reservation__actions">
                <a class="button button--primary" href="${createCarDetailsUrl(nextReservation)}">
                    Zobacz samochód
                </a>
                ${CANCELLABLE_STATUSES.has(nextReservation.status)
        ? `<button class="button button--outline" type="button" data-cancel-reservation="${escapeHtml(nextReservation.id)}">Anuluj</button>`
        : ""}
            </div>
        </div>
    `;

    container.querySelector("[data-cancel-reservation]")?.addEventListener("click", () => {
        openCancellationDialog(nextReservation);
    });
}

function renderReservations() {
    if (!reservationsList) {
        return;
    }

    const filteredReservations = reservations
        .filter(matchesSelectedFilter)
        .sort((first, second) =>
            createDate(second.startDate) - createDate(first.startDate)
        );

    reservationsList.replaceChildren();

    if (filteredReservations.length === 0) {
        reservationsList.innerHTML = `
            <div class="client-empty-state">
                <span class="client-empty-state__icon" aria-hidden="true">0</span>
                <div>
                    <h3>Brak rezerwacji w tej kategorii</h3>
                    <p>Zmień filtr albo wyszukaj samochód dostępny w wybranym terminie.</p>
                </div>
                <a class="button button--outline" href="/html/availability.html">Sprawdź dostępność</a>
            </div>
        `;
        setReservationsStatus("Nie znaleziono rezerwacji dla wybranego filtra.");
        return;
    }

    const fragment = document.createDocumentFragment();

    filteredReservations.forEach(reservation => {
        fragment.append(createReservationElement(reservation));
    });

    reservationsList.append(fragment);
    setReservationsStatus(`Liczba rezerwacji: ${filteredReservations.length}.`);
}

function createReservationElement(reservation) {
    const article = document.createElement("article");
    const imagePath = getPrimaryCarImage(reservation.carId);
    const carName = `${reservation.carBrand} ${reservation.carModel}`;
    const rentalDays = calculateRentalDays(reservation.startDate, reservation.endDate);

    article.className = "reservation-item";
    article.innerHTML = `
        <div class="reservation-item__media">
            ${imagePath
        ? `<img src="${escapeHtml(imagePath)}" alt="${escapeHtml(carName)}" loading="lazy">`
        : '<span class="reservation-image-placeholder" aria-hidden="true">CR</span>'}
        </div>

        <div class="reservation-item__content">
            <div class="reservation-item__topline">
                <div>
                    <p>Rezerwacja #${escapeHtml(reservation.id)}</p>
                    <h3>${escapeHtml(carName)}</h3>
                </div>
                <span class="reservation-status reservation-status--${getStatusClass(reservation.status)}">
                    ${escapeHtml(getStatusLabel(reservation.status))}
                </span>
            </div>

            <dl class="reservation-item__details">
                <div>
                    <dt>Termin</dt>
                    <dd>${formatDate(reservation.startDate)} – ${formatDate(reservation.endDate)}</dd>
                </div>
                <div>
                    <dt>Liczba dni</dt>
                    <dd>${rentalDays}</dd>
                </div>
                <div>
                    <dt>Wartość</dt>
                    <dd>${formatPrice(reservation.totalPrice)}</dd>
                </div>
            </dl>
        </div>

        <div class="reservation-item__actions">
            <a class="button button--outline" href="${createCarDetailsUrl(reservation)}">
                Zobacz auto
            </a>
            ${CANCELLABLE_STATUSES.has(reservation.status)
        ? `<button class="reservation-item__cancel" type="button" data-cancel-reservation="${escapeHtml(reservation.id)}">Anuluj rezerwację</button>`
        : ""}
        </div>
    `;

    return article;
}

function findNextReservation() {
    const activeReservations = reservations.filter(
        reservation => ACTIVE_STATUSES.has(reservation.status)
    );

    const inProgressReservation = activeReservations.find(
        reservation => reservation.status === "IN_PROGRESS"
    );

    if (inProgressReservation) {
        return inProgressReservation;
    }

    return activeReservations
        .filter(reservation => createDate(reservation.endDate) >= startOfToday())
        .sort((first, second) =>
            createDate(first.startDate) - createDate(second.startDate)
        )[0] ?? null;
}

function matchesSelectedFilter(reservation) {
    switch (selectedFilter) {
        case "ACTIVE":
            return ACTIVE_STATUSES.has(reservation.status);
        case "COMPLETED":
            return reservation.status === "COMPLETED";
        case "CANCELLED":
            return reservation.status === "CANCELLED";
        default:
            return true;
    }
}

function openCancellationDialog(reservation) {
    reservationToCancel = reservation;

    const description = document.querySelector("#cancel-dialog-description");
    const carName = `${reservation.carBrand} ${reservation.carModel}`;

    if (description) {
        description.textContent = `Rezerwacja #${reservation.id} (${carName}) zostanie anulowana. Tej operacji nie będzie można cofnąć.`;
    }

    if (cancelDialog && typeof cancelDialog.showModal === "function") {
        cancelDialog.showModal();
        return;
    }

    if (window.confirm(`Czy na pewno chcesz anulować rezerwację #${reservation.id}?`)) {
        confirmCancellation();
    }
}

async function confirmCancellation() {
    if (!reservationToCancel) {
        return;
    }

    const reservationId = reservationToCancel.id;
    setCancellationLoading(true);

    try {
        const cancelledReservation = await cancelReservation(reservationId);

        reservations = reservations.map(reservation =>
            Number(reservation.id) === Number(reservationId)
                ? cancelledReservation
                : reservation
        );

        cancelDialog?.close();
        renderStatistics();
        renderNextReservation();
        renderReservations();
        showToast("Rezerwacja została anulowana.");
    } catch (error) {
        cancelDialog?.close();
        setReservationsStatus(error.message || "Nie udało się anulować rezerwacji.", true);
        showToast(error.message || "Nie udało się anulować rezerwacji.", true);
    } finally {
        setCancellationLoading(false);
    }
}

function setCancellationLoading(isLoading) {
    if (!confirmCancellationButton) {
        return;
    }

    confirmCancellationButton.disabled = isLoading;
    confirmCancellationButton.textContent = isLoading
        ? "Anulowanie..."
        : "Anuluj rezerwację";
}

function createReservationCountdown(reservation) {
    if (reservation.status === "IN_PROGRESS") {
        return "Wynajem jest obecnie w trakcie";
    }

    const daysUntilStart = Math.ceil(
        (createDate(reservation.startDate) - startOfToday()) / 86400000
    );

    if (daysUntilStart <= 0) {
        return "Odbiór samochodu jest dzisiaj";
    }

    if (daysUntilStart === 1) {
        return "Odbiór samochodu jutro";
    }

    return `Odbiór samochodu za ${daysUntilStart} dni`;
}

function createCarDetailsUrl(reservation) {
    const query = new URLSearchParams({
        id: String(reservation.carId),
        startDate: reservation.startDate,
        endDate: reservation.endDate
    });

    return `/html/car-details.html?${query.toString()}`;
}

function calculateRentalDays(startDate, endDate) {
    return Math.max(
        1,
        Math.round((createDate(endDate) - createDate(startDate)) / 86400000)
    );
}

function getStatusLabel(status) {
    return statusLabels[status] ?? status?.replaceAll("_", " ") ?? "Nieznany";
}

function getStatusClass(status) {
    return String(status || "unknown").toLowerCase().replaceAll("_", "-");
}

function formatDate(dateValue) {
    if (!dateValue) {
        return "—";
    }

    return dateFormatter.format(createDate(dateValue));
}

function formatDateTime(dateValue) {
    if (!dateValue) {
        return "—";
    }

    const date = new Date(dateValue);
    return Number.isNaN(date.getTime()) ? "—" : shortDateFormatter.format(date);
}

function formatPrice(value) {
    return currencyFormatter.format(Number(value || 0));
}

function createDate(dateValue) {
    return new Date(`${dateValue}T12:00:00`);
}

function startOfToday() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return today;
}

function setText(selector, value) {
    const element = document.querySelector(selector);

    if (element) {
        element.textContent = value;
    }
}

function setPageStatus(message, isError = false) {
    if (!pageStatus) {
        return;
    }

    pageStatus.textContent = message;
    pageStatus.hidden = false;
    pageStatus.classList.toggle("client-page-status--error", isError);
}

function hidePageStatus() {
    if (pageStatus) {
        pageStatus.hidden = true;
    }
}

function setReservationsStatus(message, isError = false) {
    if (!reservationsStatus) {
        return;
    }

    reservationsStatus.textContent = message;
    reservationsStatus.classList.toggle("reservations-status--error", isError);
}

function showToast(message, isError = false) {
    const toast = document.querySelector("#client-toast");

    if (!toast) {
        return;
    }

    window.clearTimeout(toastTimeoutId);
    toast.textContent = message;
    toast.hidden = false;
    toast.classList.toggle("client-toast--error", isError);
    toast.classList.add("is-visible");

    toastTimeoutId = window.setTimeout(() => {
        toast.classList.remove("is-visible");

        window.setTimeout(() => {
            toast.hidden = true;
        }, 220);
    }, 3200);
}

function redirectToLogin() {
    window.location.replace("/html/login.html");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
