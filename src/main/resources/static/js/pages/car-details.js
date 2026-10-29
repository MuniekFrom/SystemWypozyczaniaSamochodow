import {
    createReservation,
    getAvailableCars,
    getCarAvailabilitySlots,
    getCarById
} from "/js/core/api.js";

import {
    getCarImages
} from "/js/data/car-images.js";

import {
    initPageLoader
} from "/js/core/page-loader.js";

import {
    getAccessToken,
    getRoleFromToken
} from "/js/core/auth.js";

import {
    renderSiteHeader
} from "/js/components/site-header.js";

const priceFormatter = new Intl.NumberFormat(
    "pl-PL",
    {
        style: "currency",
        currency: "PLN"
    }
);

const enumLabels = {
    GASOLINE: "Benzyna",
    DIESEL: "Diesel",
    LPG: "LPG",
    HYBRID: "Hybryda",
    PLUG_IN_HYBRID: "Hybryda plug-in",
    ELECTRIC: "Elektryczny",

    MANUAL: "Manualna",
    AUTOMATIC: "Automatyczna",
    SEMI_AUTOMATIC: "Półautomatyczna",

    FWD: "Napęd przedni",
    RWD: "Napęd tylny",
    AWD: "AWD",
    FOUR_WHEEL_DRIVE: "4×4",

    HATCHBACK: "Hatchback",
    SEDAN: "Sedan",
    STATION_WAGON: "Kombi",
    SUV: "SUV",
    MINIVAN: "Minivan",
    VAN: "Van",
    LIFTBACK: "Liftback",
    CABRIOLET: "Kabriolet",

    ECONOMY: "Ekonomiczny",
    COMPACT: "Kompakt",
    STANDARD: "Standard",
    PREMIUM: "Premium",

    ACTIVE: "Dostępny",
    IN_SERVICE: "W serwisie",
    INACTIVE: "Nieaktywny"
};

const colorLabels = {
    BLACK: "Czarny",
    WHITE: "Biały",
    SILVER: "Srebrny",
    GRAY: "Szary",
    GREY: "Szary",
    DARK_GRAY: "Ciemnoszary",
    GRAPHITE_GRAY: "Grafitowy",
    BLUE: "Niebieski",
    DARK_BLUE: "Granatowy",
    GREEN: "Zielony",
    RED: "Czerwony",
    ORANGE: "Pomarańczowy",
    YELLOW: "Żółty",
    GOLD: "Złoty",
    BROWN: "Brązowy",
    BEIGE: "Beżowy"
};

const galleryImagePositions = {
    1: [
        "center 58%",
        "center 58%",
        "center 58%",
        "center 58%"
    ],

    2: [
        "center 56%",
        "center 58%",
        "center 58%",
        "center 58%"
    ],

    3: [
        "center 72%",
        "center 66%",
        "center 68%",
        "center 70%",
        "center 66%"
    ],

    4: [
        "center 64%",
        "center 65%",
        "center 64%",
        "center 64%",
        "center 65%",
        "center 62%"
    ],

    5: [
        "center 66%",
        "center 64%",
        "center 62%"
    ]
};

let selectedCar = null;
let galleryImages = [];
let selectedImageIndex = 0;

renderSiteHeader("fleet");
initPageLoader();
initCarDetailsPage();

async function initCarDetailsPage() {
    const carId = getCarIdFromUrl();

    initializeAvailabilityForm();
    initializeGalleryDialog();

    if (!carId) {
        showPageError(
            "Nie podano prawidłowego identyfikatora samochodu."
        );

        return;
    }

    try {
        selectedCar = await getCarById(carId);

        renderCar(selectedCar);

        document.title =
            `${selectedCar.brand} ${selectedCar.model} | Car Rental`;

        hidePageStatus();
        showCarContent();

        const hasDates =
            await loadDatesFromAddress();

        if (hasDates) {
            await checkSelectedCarAvailability();
        }
    } catch (error) {
        showPageError(error.message);
    }
}

function getCarIdFromUrl() {
    const query =
        new URLSearchParams(
            window.location.search
        );

    const carId =
        Number(query.get("id"));

    if (
        !Number.isInteger(carId)
        || carId <= 0
    ) {
        return null;
    }

    return carId;
}

function renderCar(car) {
    setText(
        "#car-name",
        `${car.brand} ${car.model}`
    );

    setText(
        "#car-category",
        getEnumLabel(car.category)
    );

    setText(
        "#car-status",
        getEnumLabel(car.status)
    );

    setText(
        "#car-subtitle",
        createCarSubtitle(car)
    );

    setText(
        "#car-description",
        car.description
        || "Brak dodatkowego opisu samochodu."
    );

    setText(
        "#car-daily-price",
        priceFormatter.format(
            Number(car.dailyPrice)
        )
    );

    updateStatusAppearance(car.status);
    renderGallery(car);
    renderSpecifications(car);
}

function createCarSubtitle(car) {
    const values = [
        car.productionYear,
        getEnumLabel(car.bodyType),
        getEnumLabel(car.fuelType),
        formatEngineCapacity(
            car.engineCapacityCm3
        )
    ];

    return values
        .filter(Boolean)
        .join(" · ");
}

function renderGallery(car) {
    galleryImages =
        getCarImages(car.id);

    selectedImageIndex = 0;

    const mainImage =
        document.querySelector(
            "#car-main-image"
        );

    const placeholder =
        document.querySelector(
            "#car-image-placeholder"
        );

    const openButton =
        document.querySelector(
            "#gallery-open"
        );

    const thumbnails =
        document.querySelector(
            "#car-thumbnails"
        );

    thumbnails?.replaceChildren();

    if (
        galleryImages.length === 0
        || !mainImage
        || !placeholder
    ) {
        mainImage?.setAttribute(
            "hidden",
            ""
        );

        placeholder?.removeAttribute(
            "hidden"
        );

        if (openButton) {
            openButton.disabled = true;
        }

        return;
    }

    mainImage.removeAttribute("hidden");
    placeholder.setAttribute("hidden", "");

    if (openButton) {
        openButton.disabled = false;
    }

    galleryImages.forEach(
        (imagePath, index) => {
            const button =
                document.createElement("button");

            button.className =
                "car-gallery__thumbnail";

            button.type = "button";

            button.setAttribute(
                "aria-label",
                `Pokaż zdjęcie ${index + 1}`
            );

            const image =
                document.createElement("img");

            image.src = imagePath;

            image.alt =
                `${car.brand} ${car.model} – zdjęcie ${index + 1}`;

            image.loading = "lazy";

            button.append(image);

            button.addEventListener(
                "click",
                () => showGalleryImage(index)
            );

            thumbnails?.append(button);
        }
    );

    showGalleryImage(0);
}

function showGalleryImage(index) {
    if (galleryImages.length === 0) {
        return;
    }

    const normalizedIndex =
        (
            index
            + galleryImages.length
        ) % galleryImages.length;

    selectedImageIndex =
        normalizedIndex;

    const mainImage =
        document.querySelector(
            "#car-main-image"
        );

    if (mainImage && selectedCar) {
        const selectedImage =
            galleryImages[normalizedIndex];

        mainImage.src =
            selectedImage;

        mainImage.alt =
            `${selectedCar.brand} ${selectedCar.model} – zdjęcie ${normalizedIndex + 1}`;

        mainImage.style.objectPosition =
            getGalleryImagePosition(
                selectedCar.id,
                normalizedIndex
            );
    }

    const thumbnailButtons =
        document.querySelectorAll(
            ".car-gallery__thumbnail"
        );

    thumbnailButtons.forEach(
        (button, buttonIndex) => {
            const isActive =
                buttonIndex === normalizedIndex;

            button.classList.toggle(
                "is-active",
                isActive
            );

            if (isActive) {
                button.setAttribute(
                    "aria-current",
                    "true"
                );
            } else {
                button.removeAttribute(
                    "aria-current"
                );
            }
        }
    );

    updateDialogImage();
}

function renderSpecifications(car) {
    const specifications =
        document.querySelector(
            "#car-specifications"
        );

    if (!specifications) {
        return;
    }

    specifications.replaceChildren();

    const values = [
        {
            label: "Rok produkcji",
            value: car.productionYear
        },
        {
            label: "Nadwozie",
            value: getEnumLabel(
                car.bodyType
            )
        },
        {
            label: "Paliwo",
            value: getEnumLabel(
                car.fuelType
            )
        },
        {
            label: "Pojemność silnika",
            value: formatEngineCapacity(
                car.engineCapacityCm3
            )
        },
        {
            label: "Moc",
            value: car.powerHp
                ? `${car.powerHp} KM`
                : "Brak danych"
        },
        {
            label: "Napęd",
            value: getEnumLabel(
                car.driveType
            )
        },
        {
            label: "Skrzynia biegów",
            value: getEnumLabel(
                car.transmissionType
            )
        },
        {
            label: "Liczba miejsc",
            value: car.numberOfSeats
                ?? "Brak danych"
        },
        {
            label: "Przyspieszenie 0–100",
            value:
                car.accelerationTo100Seconds
                    ? `${formatAcceleration(
                        car.accelerationTo100Seconds
                    )} s`
                    : "Brak danych"
        },
        {
            label: "Kolor",
            value: getColorLabel(car.color)
        }
    ];

    values.forEach(item => {
        const wrapper =
            document.createElement("div");

        wrapper.className =
            "technical-item";

        const term =
            document.createElement("dt");

        term.textContent =
            item.label;

        const description =
            document.createElement("dd");

        description.textContent =
            String(
                item.value
                ?? "Brak danych"
            );

        wrapper.append(
            term,
            description
        );

        specifications.append(wrapper);
    });
}

function initializeAvailabilityForm() {
    const form = document.querySelector("#car-availability-form");
    const startDateInput = document.querySelector("#details-start-date");
    const startTimeInput = document.querySelector("#details-start-time");
    const endDateInput = document.querySelector("#details-end-date");
    const endTimeInput = document.querySelector("#details-end-time");

    if (
        !form
        || !startDateInput
        || !startTimeInput
        || !endDateInput
        || !endTimeInput
    ) {
        return;
    }

    const today = formatDate(new Date());
    startDateInput.min = today;
    endDateInput.min = today;

    startDateInput.addEventListener("change", async () => {
        updateEndDateMinimum(startDateInput, endDateInput);
        await refreshDateSlots("start");

        if (!endDateInput.value) {
            endDateInput.value = startDateInput.value;
        }

        await refreshDateSlots("end");
        updateEndTimeOptions();
        updatePriceSummary();
        resetAvailabilityResult();
    });

    startTimeInput.addEventListener("change", () => {
        updateEndTimeOptions();
        updatePriceSummary();
        resetAvailabilityResult();
    });

    endDateInput.addEventListener("change", async () => {
        await refreshDateSlots("end");
        updateEndTimeOptions();
        updatePriceSummary();
        resetAvailabilityResult();
    });

    endTimeInput.addEventListener("change", () => {
        updatePriceSummary();
        resetAvailabilityResult();
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();
        await checkSelectedCarAvailability();
    });

    document
        .querySelector("#reservation-link")
        ?.addEventListener("click", handleReservationAction);
}

function getColorLabel(color) {
    if (!color) {
        return "Brak danych";
    }

    const normalizedColor = color
        .trim()
        .toUpperCase()
        .replaceAll("-", "_")
        .replaceAll(" ", "_");

    return colorLabels[normalizedColor] ?? color;
}

async function loadDatesFromAddress() {
    const query = new URLSearchParams(window.location.search);
    const start = splitDateTime(query.get("startAt"));
    const end = splitDateTime(query.get("endAt"));
    const startDateInput = document.querySelector("#details-start-date");
    const startTimeInput = document.querySelector("#details-start-time");
    const endDateInput = document.querySelector("#details-end-date");
    const endTimeInput = document.querySelector("#details-end-time");

    if (
        !start
        || !end
        || !startDateInput
        || !startTimeInput
        || !endDateInput
        || !endTimeInput
    ) {
        return false;
    }

    const startAt = `${start.date}T${start.time}`;
    const endAt = `${end.date}T${end.time}`;

    if (endAt <= startAt || start.date < startDateInput.min) {
        return false;
    }

    startDateInput.value = start.date;
    updateEndDateMinimum(startDateInput, endDateInput);
    endDateInput.value = end.date;

    await refreshDateSlots("start", start.time);
    await refreshDateSlots("end", end.time);
    updateEndTimeOptions();

    if (
        startTimeInput.value !== start.time
        || endTimeInput.value !== end.time
    ) {
        resetAvailabilityResult();
        return false;
    }

    updatePriceSummary();
    return true;
}

async function checkSelectedCarAvailability() {
    if (!selectedCar) {
        return;
    }

    const period = getSelectedPeriod();

    if (!period) {
        showAvailabilityMessage(
            "Wybierz datę i godzinę odbioru oraz zwrotu.",
            "error"
        );

        return;
    }

    if (period.endAt <= period.startAt) {
        showAvailabilityMessage(
            "Termin zwrotu musi przypadać po terminie odbioru.",
            "error"
        );

        return;
    }

    if (selectedCar.status !== "ACTIVE") {
        showAvailabilityMessage(
            "Ten samochód nie jest obecnie dostępny do wynajęcia.",
            "error"
        );

        hideReservationLink();

        return;
    }

    const submitButton =
        document.querySelector(
            "#availability-submit"
        );

    if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent =
            "Sprawdzanie...";
    }

    showAvailabilityMessage(
        "Sprawdzanie dostępności samochodu...",
        "loading"
    );

    hideReservationLink();

    try {
        const availableCars =
            await getAvailableCars(
                period.startAt,
                period.endAt
            );

        const isAvailable =
            availableCars.some(
                car =>
                    Number(car.id)
                    === Number(selectedCar.id)
            );

        updateAddressDates(
            period.startAt,
            period.endAt
        );

        updatePriceSummary();

        if (isAvailable) {
            showAvailabilityMessage(
                "Samochód jest dostępny w wybranym terminie.",
                "success"
            );

            showReservationLink();
        } else {
            showAvailabilityMessage(
                "Samochód jest już zajęty w wybranym terminie.",
                "error"
            );
        }
    } catch (error) {
        showAvailabilityMessage(
            error.message,
            "error"
        );
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent =
                "Sprawdź dostępność";
        }
    }
}

function updatePriceSummary() {
    if (!selectedCar) {
        return;
    }

    const period = getSelectedPeriod();
    const summary = document.querySelector("#price-summary");

    if (
        !period
        || period.endAt <= period.startAt
    ) {
        summary?.setAttribute("hidden", "");
        return;
    }

    const rentalDays =
        calculateRentalDays(
            period.startAt,
            period.endAt
        );

    const totalPrice =
        rentalDays
        * Number(selectedCar.dailyPrice);

    setText(
        "#rental-days",
        String(rentalDays)
    );

    setText(
        "#rental-total-price",
        priceFormatter.format(totalPrice)
    );

    summary?.removeAttribute("hidden");
}

function calculateRentalDays(
    startAt,
    endAt
) {
    const millisecondsPerDay = 24 * 60 * 60 * 1000;
    const duration = getLocalDateTimeValue(endAt)
        - getLocalDateTimeValue(startAt);

    return Math.max(
        1,
        Math.ceil(duration / millisecondsPerDay)
    );
}

function getLocalDateTimeValue(value) {
    const [datePart, timePart] = value.split("T");
    const [year, month, day] = datePart.split("-").map(Number);
    const [hour, minute] = timePart.split(":").map(Number);

    return Date.UTC(year, month - 1, day, hour, minute);
}

function updateEndDateMinimum(
    startDateInput,
    endDateInput
) {
    if (!startDateInput.value) {
        return;
    }

    const minimumEndDate = startDateInput.value;

    endDateInput.min =
        minimumEndDate;

    if (
        endDateInput.value
        && endDateInput.value
        < minimumEndDate
    ) {
        endDateInput.value =
            minimumEndDate;
    }
}

function updateAddressDates(
    startAt,
    endAt
) {
    const query =
        new URLSearchParams(
            window.location.search
        );

    query.set(
        "id",
        String(selectedCar.id)
    );

    query.set(
        "startAt",
        startAt
    );

    query.set(
        "endAt",
        endAt
    );

    window.history.replaceState(
        null,
        "",
        `${window.location.pathname}?${query.toString()}`
    );
}

async function handleReservationAction(event) {
    event.preventDefault();

    if (!selectedCar) {
        return;
    }

    const period = getSelectedPeriod();

    if (!period || period.endAt <= period.startAt) {
        showAvailabilityMessage(
            "Najpierw wybierz termin wynajmu.",
            "error"
        );

        return;
    }

    const accessToken = getAccessToken();

    if (!accessToken) {
        savePendingReservation();

        const returnUrl =
            `${window.location.pathname}${window.location.search}`;

        window.location.assign(
            `/html/login.html?redirect=${encodeURIComponent(returnUrl)}`
        );

        return;
    }

    const role = getRoleFromToken(accessToken);

    if (role !== "CLIENT") {
        showAvailabilityMessage(
            "Rezerwację może utworzyć wyłącznie klient.",
            "error"
        );

        return;
    }

    const reservationLink = event.currentTarget;

    if (reservationLink.dataset.loading === "true") {
        return;
    }

    reservationLink.dataset.loading = "true";
    reservationLink.setAttribute("aria-disabled", "true");
    reservationLink.textContent = "Tworzenie rezerwacji...";

    try {
        const createdReservation =
            await createReservation({
                carId: selectedCar.id,
                startAt: period.startAt,
                endAt: period.endAt
            });

        sessionStorage.removeItem(
            "pendingReservation"
        );

        showAvailabilityMessage(
            "Rezerwacja została utworzona.",
            "success"
        );

        window.setTimeout(() => {
            window.location.assign(
                `/html/client.html?reservationCreated=${createdReservation.id}`
            );
        }, 600);
    } catch (error) {
        showAvailabilityMessage(
            error.message
            || "Nie udało się utworzyć rezerwacji.",
            "error"
        );
    } finally {
        delete reservationLink.dataset.loading;
        reservationLink.removeAttribute("aria-disabled");
        reservationLink.textContent = "Zarezerwuj samochód";
    }
}

function savePendingReservation() {
    if (!selectedCar) {
        return;
    }

    const period = getSelectedPeriod();

    if (!period) {
        return;
    }

    sessionStorage.setItem(
        "pendingReservation",
        JSON.stringify({
            carId: selectedCar.id,
            startAt: period.startAt,
            endAt: period.endAt
        })
    );
}

async function refreshDateSlots(kind, preferredTime = null) {
    if (!selectedCar) {
        return;
    }

    const isStart = kind === "start";
    const dateInput = document.querySelector(
        isStart ? "#details-start-date" : "#details-end-date"
    );
    const timeSelect = document.querySelector(
        isStart ? "#details-start-time" : "#details-end-time"
    );

    if (!dateInput?.value || !timeSelect) {
        return;
    }

    const previousValue = preferredTime ?? timeSelect.value;
    timeSelect.disabled = true;
    timeSelect.replaceChildren(new Option("Ładowanie godzin...", ""));

    try {
        const slots = await getCarAvailabilitySlots(
            selectedCar.id,
            dateInput.value
        );

        timeSelect.replaceChildren();

        slots.forEach(slot => {
            const option = new Option(
                slot.available ? slot.time : `${slot.time} – niedostępna`,
                slot.time
            );

            option.disabled = !slot.available;
            option.dataset.apiAvailable = String(slot.available);
            timeSelect.add(option);
        });

        const preferredOption = Array.from(timeSelect.options).find(
            option => option.value === previousValue && !option.disabled
        );

        timeSelect.value = preferredOption?.value ?? "";

        if (!timeSelect.value) {
            selectFirstEnabledOption(timeSelect);
        }
    } catch (error) {
        timeSelect.replaceChildren(
            new Option("Nie udało się pobrać godzin", "")
        );

        showAvailabilityMessage(
            error.message || "Nie udało się pobrać dostępnych godzin.",
            "error"
        );
    } finally {
        timeSelect.disabled = false;
    }
}

function updateEndTimeOptions() {
    const startDate = document.querySelector("#details-start-date")?.value;
    const startTime = document.querySelector("#details-start-time")?.value;
    const endDate = document.querySelector("#details-end-date")?.value;
    const endTimeSelect = document.querySelector("#details-end-time");

    if (!startDate || !startTime || !endDate || !endTimeSelect) {
        return;
    }

    const sameDay = startDate === endDate;

    Array.from(endTimeSelect.options).forEach(option => {
        const apiAvailable = option.dataset.apiAvailable !== "false";
        const afterStart = !sameDay || option.value > startTime;
        option.disabled = !apiAvailable || !afterStart;

        option.textContent = apiAvailable
            ? option.value
            : `${option.value} – niedostępna`;
    });

    if (endTimeSelect.selectedOptions[0]?.disabled) {
        selectFirstEnabledOption(endTimeSelect);
    }
}

function selectFirstEnabledOption(select) {
    const option = Array.from(select.options).find(item => !item.disabled);
    select.value = option?.value ?? "";
}

function getSelectedPeriod() {
    const startDate = document.querySelector("#details-start-date")?.value;
    const startTime = document.querySelector("#details-start-time")?.value;
    const endDate = document.querySelector("#details-end-date")?.value;
    const endTime = document.querySelector("#details-end-time")?.value;

    if (!startDate || !startTime || !endDate || !endTime) {
        return null;
    }

    return {
        startAt: `${startDate}T${startTime}`,
        endAt: `${endDate}T${endTime}`
    };
}

function splitDateTime(value) {
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value ?? "")) {
        return null;
    }

    const [date, time] = value.split("T");
    return { date, time };
}

function showAvailabilityMessage(
    message,
    type
) {
    const element =
        document.querySelector(
            "#availability-message"
        );

    if (!element) {
        return;
    }

    element.textContent = message;
    element.className = "details-message";

    if (type === "success") {
        element.classList.add(
            "message--success"
        );
    } else if (type === "error") {
        element.classList.add(
            "message--error"
        );
    } else {
        element.classList.add(
            "details-message--loading"
        );
    }

    element.removeAttribute("hidden");
}

function resetAvailabilityResult() {
    const message =
        document.querySelector(
            "#availability-message"
        );

    message?.setAttribute(
        "hidden",
        ""
    );

    hideReservationLink();
}

function showReservationLink() {
    const reservationLink =
        document.querySelector(
            "#reservation-link"
        );

    if (!reservationLink) {
        return;
    }

    const accessToken = getAccessToken();
    const role = accessToken
        ? getRoleFromToken(accessToken)
        : null;

    if (role === "CLIENT") {
        reservationLink.textContent =
            "Zarezerwuj samochód";

        reservationLink.href = "#";
    } else {
        reservationLink.textContent =
            "Zaloguj się i zarezerwuj";

        reservationLink.href =
            "/html/login.html";
    }

    reservationLink.removeAttribute("hidden");
}

function hideReservationLink() {
    document
        .querySelector("#reservation-link")
        ?.setAttribute("hidden", "");
}

function initializeGalleryDialog() {
    const dialog =
        document.querySelector(
            "#gallery-dialog"
        );

    const openButton =
        document.querySelector(
            "#gallery-open"
        );

    const closeButton =
        document.querySelector(
            "#gallery-close"
        );

    const previousButton =
        document.querySelector(
            "#gallery-previous"
        );

    const nextButton =
        document.querySelector(
            "#gallery-next"
        );

    openButton?.addEventListener(
        "click",
        () => {
            if (galleryImages.length === 0) {
                return;
            }

            updateDialogImage();

            if (
                dialog
                && typeof dialog.showModal
                === "function"
            ) {
                dialog.showModal();
            } else {
                window.open(
                    galleryImages[selectedImageIndex],
                    "_blank",
                    "noopener"
                );
            }
        }
    );

    closeButton?.addEventListener(
        "click",
        () => dialog?.close()
    );

    previousButton?.addEventListener(
        "click",
        () => {
            showGalleryImage(
                selectedImageIndex - 1
            );
        }
    );

    nextButton?.addEventListener(
        "click",
        () => {
            showGalleryImage(
                selectedImageIndex + 1
            );
        }
    );

    dialog?.addEventListener(
        "click",
        event => {
            if (event.target === dialog) {
                dialog.close();
            }
        }
    );

    document.addEventListener(
        "keydown",
        event => {
            if (!dialog?.open) {
                return;
            }

            if (event.key === "ArrowLeft") {
                showGalleryImage(
                    selectedImageIndex - 1
                );
            }

            if (event.key === "ArrowRight") {
                showGalleryImage(
                    selectedImageIndex + 1
                );
            }
        }
    );
}

function updateDialogImage() {
    const dialogImage =
        document.querySelector(
            "#gallery-dialog-image"
        );

    const counter =
        document.querySelector(
            "#gallery-counter"
        );

    const previousButton =
        document.querySelector(
            "#gallery-previous"
        );

    const nextButton =
        document.querySelector(
            "#gallery-next"
        );

    if (
        !dialogImage
        || galleryImages.length === 0
        || !selectedCar
    ) {
        return;
    }

    dialogImage.src =
        galleryImages[selectedImageIndex];

    dialogImage.alt =
        `${selectedCar.brand} ${selectedCar.model} – zdjęcie ${selectedImageIndex + 1}`;

    if (counter) {
        counter.textContent =
            `${selectedImageIndex + 1} / ${galleryImages.length}`;
    }

    const hasMultipleImages =
        galleryImages.length > 1;

    if (previousButton) {
        previousButton.disabled =
            !hasMultipleImages;
    }

    if (nextButton) {
        nextButton.disabled =
            !hasMultipleImages;
    }
}

function updateStatusAppearance(status) {
    const statusElement =
        document.querySelector(
            "#car-status"
        );

    if (!statusElement) {
        return;
    }

    statusElement.classList.toggle(
        "car-summary__status--unavailable",
        status !== "ACTIVE"
    );
}

function showPageError(message) {
    const status =
        document.querySelector(
            "#details-status"
        );

    if (!status) {
        return;
    }

    status.textContent = message;

    status.classList.add(
        "details-status--error"
    );

    status.hidden = false;

    document
        .querySelector("#car-details-content")
        ?.setAttribute("hidden", "");
}

function hidePageStatus() {
    const status =
        document.querySelector(
            "#details-status"
        );

    if (status) {
        status.hidden = true;
    }
}

function showCarContent() {
    document
        .querySelector("#car-details-content")
        ?.removeAttribute("hidden");
}

function setText(selector, value) {
    const element =
        document.querySelector(selector);

    if (element) {
        element.textContent =
            value ?? "Brak danych";
    }
}

function getEnumLabel(value) {
    if (!value) {
        return "Brak danych";
    }

    return enumLabels[value]
        ?? value
            .replaceAll("_", " ")
            .toLowerCase();
}

function formatEngineCapacity(capacityCm3) {
    if (!capacityCm3) {
        return "Brak danych";
    }

    const capacityLiters =
        capacityCm3 / 1000;

    return `${capacityLiters.toLocaleString(
        "pl-PL",
        {
            minimumFractionDigits: 1,
            maximumFractionDigits: 1
        }
    )} l`;
}

function formatAcceleration(value) {
    return Number(value).toLocaleString(
        "pl-PL",
        {
            minimumFractionDigits: 1,
            maximumFractionDigits: 2
        }
    );
}

function formatDate(date) {
    const year =
        date.getFullYear();

    const month =
        String(
            date.getMonth() + 1
        ).padStart(2, "0");

    const day =
        String(
            date.getDate()
        ).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function getGalleryImagePosition(
    carId,
    imageIndex
) {
    return galleryImagePositions[carId]?.[imageIndex]
        ?? "center";
}
