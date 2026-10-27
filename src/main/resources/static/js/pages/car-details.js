import {
    getAvailableCars,
    getCarById
} from "/js/core/api.js";

import {
    getCarImages
} from "/js/data/car-images.js";

import {
    initPageLoader
} from "/js/core/page-loader.js";

import {
    initThemeSettings
} from "/js/core/theme.js";

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

initThemeSettings();
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
            loadDatesFromAddress();

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
    const form =
        document.querySelector(
            "#car-availability-form"
        );

    const startDateInput =
        document.querySelector(
            "#details-start-date"
        );

    const endDateInput =
        document.querySelector(
            "#details-end-date"
        );

    if (
        !form
        || !startDateInput
        || !endDateInput
    ) {
        return;
    }

    const today =
        formatDate(new Date());

    startDateInput.min = today;
    endDateInput.min = today;

    startDateInput.addEventListener(
        "change",
        () => {
            updateEndDateMinimum(
                startDateInput,
                endDateInput
            );

            updatePriceSummary();
            resetAvailabilityResult();
        }
    );

    endDateInput.addEventListener(
        "change",
        () => {
            updatePriceSummary();
            resetAvailabilityResult();
        }
    );

    form.addEventListener(
        "submit",
        async event => {
            event.preventDefault();

            await checkSelectedCarAvailability();
        }
    );

    document
        .querySelector("#reservation-link")
        ?.addEventListener(
            "click",
            savePendingReservation
        );
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

function loadDatesFromAddress() {
    const query =
        new URLSearchParams(
            window.location.search
        );

    const startDate =
        query.get("startDate");

    const endDate =
        query.get("endDate");

    const startDateInput =
        document.querySelector(
            "#details-start-date"
        );

    const endDateInput =
        document.querySelector(
            "#details-end-date"
        );

    if (
        !startDate
        || !endDate
        || !startDateInput
        || !endDateInput
    ) {
        return false;
    }

    if (
        endDate <= startDate
        || startDate < startDateInput.min
    ) {
        return false;
    }

    startDateInput.value =
        startDate;

    updateEndDateMinimum(
        startDateInput,
        endDateInput
    );

    endDateInput.value =
        endDate;

    updatePriceSummary();

    return true;
}

async function checkSelectedCarAvailability() {
    if (!selectedCar) {
        return;
    }

    const startDate =
        document.querySelector(
            "#details-start-date"
        )?.value;

    const endDate =
        document.querySelector(
            "#details-end-date"
        )?.value;

    if (!startDate || !endDate) {
        showAvailabilityMessage(
            "Wybierz datę odbioru i zwrotu.",
            "error"
        );

        return;
    }

    if (endDate <= startDate) {
        showAvailabilityMessage(
            "Data zwrotu musi przypadać po dacie odbioru.",
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
                startDate,
                endDate
            );

        const isAvailable =
            availableCars.some(
                car =>
                    Number(car.id)
                    === Number(selectedCar.id)
            );

        updateAddressDates(
            startDate,
            endDate
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

    const startDate =
        document.querySelector(
            "#details-start-date"
        )?.value;

    const endDate =
        document.querySelector(
            "#details-end-date"
        )?.value;

    const summary =
        document.querySelector(
            "#price-summary"
        );

    if (
        !startDate
        || !endDate
        || endDate <= startDate
    ) {
        summary?.setAttribute(
            "hidden",
            ""
        );

        return;
    }

    const rentalDays =
        calculateRentalDays(
            startDate,
            endDate
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
    startDate,
    endDate
) {
    const start =
        createUtcDate(startDate);

    const end =
        createUtcDate(endDate);

    const millisecondsPerDay =
        24 * 60 * 60 * 1000;

    return Math.round(
        (end - start) / millisecondsPerDay
    );
}

function createUtcDate(dateValue) {
    const [
        year,
        month,
        day
    ] = dateValue
        .split("-")
        .map(Number);

    return Date.UTC(
        year,
        month - 1,
        day
    );
}

function updateEndDateMinimum(
    startDateInput,
    endDateInput
) {
    if (!startDateInput.value) {
        return;
    }

    const minimumEndDate =
        addDays(
            startDateInput.value,
            1
        );

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
    startDate,
    endDate
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
        "startDate",
        startDate
    );

    query.set(
        "endDate",
        endDate
    );

    window.history.replaceState(
        null,
        "",
        `${window.location.pathname}?${query.toString()}`
    );
}

function savePendingReservation() {
    if (!selectedCar) {
        return;
    }

    const startDate =
        document.querySelector(
            "#details-start-date"
        )?.value;

    const endDate =
        document.querySelector(
            "#details-end-date"
        )?.value;

    if (!startDate || !endDate) {
        return;
    }

    sessionStorage.setItem(
        "pendingReservation",
        JSON.stringify({
            carId: selectedCar.id,
            startDate,
            endDate
        })
    );
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
    document
        .querySelector("#reservation-link")
        ?.removeAttribute("hidden");
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

function addDays(dateValue, days) {
    const date =
        new Date(
            `${dateValue}T12:00:00`
        );

    date.setDate(
        date.getDate() + days
    );

    return formatDate(date);
}

function getGalleryImagePosition(
    carId,
    imageIndex
) {
    return galleryImagePositions[carId]?.[imageIndex]
        ?? "center";
}