import {
    getAvailableCars,
    getCars
} from "/js/core/api.js";

import {
    initPageLoader
} from "/js/core/page-loader.js";

import {
    initThemeSettings
} from "/js/core/theme.js";

initThemeSettings();
initPageLoader();
initHomePage();

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
    ELECTRIC: "Elektryczny",

    MANUAL: "Manualna",
    AUTOMATIC: "Automatyczna",

    COMPACT: "Kompakt",
    STANDARD: "Standard",
    PREMIUM: "Premium",
    SUV: "SUV",
    VAN: "Van"
};

async function initHomePage() {
    const form =
        document.querySelector("#availability-form");

    const startDateInput =
        document.querySelector("#start-date");

    const endDateInput =
        document.querySelector("#end-date");

    if (!form || !startDateInput || !endDateInput) {
        return;
    }

    setDateConstraints(
        startDateInput,
        endDateInput
    );

    startDateInput.addEventListener("change", () => {
        updateEndDateMinimum(
            startDateInput,
            endDateInput
        );
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();

        await searchAvailableCars(
            startDateInput.value,
            endDateInput.value
        );
    });

    await loadFleet();
}

function setDateConstraints(
    startDateInput,
    endDateInput
) {
    const today = new Date();
    const todayValue = formatDate(today);

    startDateInput.min = todayValue;
    endDateInput.min = todayValue;
}

function updateEndDateMinimum(
    startDateInput,
    endDateInput
) {
    if (!startDateInput.value) {
        return;
    }

    const minimumEndDate =
        addDays(startDateInput.value, 1);

    endDateInput.min = minimumEndDate;

    if (
        endDateInput.value
        && endDateInput.value < minimumEndDate
    ) {
        endDateInput.value = minimumEndDate;
    }
}

async function loadFleet() {
    setFleetStatus(
        "Ładowanie samochodów..."
    );

    try {
        const cars = await getCars();

        const activeCars = cars.filter(
            car => !car.status || car.status === "ACTIVE"
        );

        renderCars(activeCars);

        if (activeCars.length === 0) {
            setFleetStatus(
                "Obecnie nie ma aktywnych samochodów."
            );

            return;
        }

        setFleetStatus(
            `Liczba samochodów we flocie: ${activeCars.length}.`
        );
    } catch (error) {
        setFleetError(error.message);
    }
}

async function searchAvailableCars(
    startDate,
    endDate
) {
    if (!startDate || !endDate) {
        setFleetError(
            "Wybierz datę odbioru i zwrotu."
        );

        return;
    }

    if (endDate <= startDate) {
        setFleetError(
            "Data zwrotu musi przypadać po dacie odbioru."
        );

        return;
    }

    setFleetStatus(
        "Sprawdzanie dostępności samochodów..."
    );

    try {
        const cars = await getAvailableCars(
            startDate,
            endDate
        );

        renderCars(cars);

        if (cars.length === 0) {
            setFleetStatus(
                "Brak dostępnych samochodów w wybranym terminie."
            );
        } else {
            setFleetStatus(
                `Dostępne samochody: ${cars.length}.`
            );
        }

        document
            .querySelector("#cars")
            ?.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
    } catch (error) {
        setFleetError(error.message);
    }
}

function renderCars(cars) {
    const carsList =
        document.querySelector("#cars-list");

    if (!carsList) {
        return;
    }

    carsList.replaceChildren();

    const fragment =
        document.createDocumentFragment();

    cars.forEach(car => {
        fragment.append(
            createCarCard(car)
        );
    });

    carsList.append(fragment);
}

function createCarCard(car) {
    const article =
        document.createElement("article");

    article.className = "car-card";

    const top =
        document.createElement("div");

    top.className = "car-card__top";

    top.append(
        createTextElement(
            "span",
            "car-card__category",
            getEnumLabel(car.category)
        ),
        createTextElement(
            "span",
            "car-card__status",
            "Dostępny"
        )
    );

    const title = createTextElement(
        "h3",
        "car-card__title",
        `${car.brand} ${car.model}`
    );

    const subtitle = createTextElement(
        "p",
        "car-card__subtitle",
        `${car.productionYear} · ${car.color}`
    );

    const specifications =
        document.createElement("ul");

    specifications.className =
        "car-card__specifications";

    specifications.append(
        createTextElement(
            "li",
            "",
            `Paliwo: ${getEnumLabel(car.fuelType)}`
        ),
        createTextElement(
            "li",
            "",
            `Skrzynia: ${getEnumLabel(car.transmissionType)}`
        ),
        createTextElement(
            "li",
            "",
            `Liczba miejsc: ${car.numberOfSeats}`
        )
    );

    const footer =
        document.createElement("div");

    footer.className = "car-card__footer";

    const price =
        document.createElement("div");

    price.className = "car-card__price";

    price.append(
        createTextElement(
            "strong",
            "",
            priceFormatter.format(car.dailyPrice)
        ),
        createTextElement(
            "span",
            "",
            "za dobę"
        )
    );

    const loginLink =
        document.createElement("a");

    loginLink.className =
        "button button--outline";

    loginLink.href = "/html/login.html";
    loginLink.textContent = "Zarezerwuj";

    footer.append(
        price,
        loginLink
    );

    article.append(
        top,
        title,
        subtitle,
        specifications
    );

    if (car.description) {
        article.append(
            createTextElement(
                "p",
                "car-card__description",
                car.description
            )
        );
    }

    article.append(footer);

    return article;
}

function createTextElement(
    tagName,
    className,
    text
) {
    const element =
        document.createElement(tagName);

    if (className) {
        element.className = className;
    }

    element.textContent = text;

    return element;
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

function setFleetStatus(message) {
    const status =
        document.querySelector("#cars-status");

    if (!status) {
        return;
    }

    status.textContent = message;
    status.classList.remove(
        "fleet__status--error"
    );
}

function setFleetError(message) {
    const status =
        document.querySelector("#cars-status");

    if (!status) {
        return;
    }

    status.textContent = message;
    status.classList.add(
        "fleet__status--error"
    );
}

function formatDate(date) {
    const year = date.getFullYear();

    const month = String(
        date.getMonth() + 1
    ).padStart(2, "0");

    const day = String(
        date.getDate()
    ).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function addDays(dateValue, days) {
    const date = new Date(
        `${dateValue}T12:00:00`
    );

    date.setDate(
        date.getDate() + days
    );

    return formatDate(date);
}