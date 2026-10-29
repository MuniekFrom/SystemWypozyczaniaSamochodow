import { getPrimaryCarImage } from "/js/data/car-images.js";

const priceFormatter = new Intl.NumberFormat("pl-PL", {
    style: "currency",
    currency: "PLN"
});

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
    FWD: "Przedni",
    RWD: "Tylny",
    AWD: "AWD",
    FOUR_WHEEL_DRIVE: "4×4",
    ECONOMY: "Ekonomiczny",
    COMPACT: "Kompakt",
    STANDARD: "Standard",
    PREMIUM: "Premium"
};

export function renderCarCards(container, cars, options = {}) {
    container.replaceChildren();

    const fragment = document.createDocumentFragment();

    cars.forEach(car => {
        fragment.append(createCarCard(car, options));
    });

    container.append(fragment);
}

export function createCarCard(car, options = {}) {
    const detailsUrl = createCarDetailsUrl(car.id, options);
    const article = document.createElement("article");
    article.className = "car-card";
    article.dataset.carId = String(car.id);

    const media = document.createElement("a");
    media.className = "car-card__media";
    media.href = detailsUrl;
    media.setAttribute("aria-label", `Zobacz samochód ${car.brand} ${car.model}`);

    const imageUrl = getPrimaryCarImage(car.id);

    if (imageUrl) {
        const image = document.createElement("img");
        image.className = "car-card__image";
        image.src = imageUrl;
        image.alt = `${car.brand} ${car.model}`;
        image.loading = "lazy";
        media.append(image);
    } else {
        media.append(createTextElement("div", "car-card__image-placeholder", "Brak zdjęcia"));
    }

    const badges = document.createElement("div");
    badges.className = "car-card__badges";
    badges.append(
        createTextElement("span", "car-card__category", getEnumLabel(car.category)),
        createTextElement("span", "car-card__status", "Dostępny")
    );
    media.append(badges);

    const content = document.createElement("div");
    content.className = "car-card__content";

    const title = document.createElement("h3");
    title.className = "car-card__title";

    const titleLink = document.createElement("a");
    titleLink.href = detailsUrl;
    titleLink.textContent = `${car.brand} ${car.model}`;
    title.append(titleLink);

    const specifications = document.createElement("dl");
    specifications.className = "car-card__specifications";
    specifications.append(
        createSpecification("Moc", car.powerHp ? `${car.powerHp} KM` : "Brak danych"),
        createSpecification(
            "0–100",
            car.accelerationTo100Seconds
                ? `${formatAcceleration(car.accelerationTo100Seconds)} s`
                : "Brak danych"
        ),
        createSpecification("Napęd", getEnumLabel(car.driveType)),
        createSpecification("Skrzynia", getEnumLabel(car.transmissionType))
    );

    const footer = document.createElement("div");
    footer.className = "car-card__footer";

    const price = document.createElement("div");
    price.className = "car-card__price";
    price.append(
        createTextElement("span", "", "od"),
        createTextElement("strong", "", priceFormatter.format(Number(car.dailyPrice))),
        createTextElement("span", "", "za dobę")
    );

    const detailsLink = document.createElement("a");
    detailsLink.className = "button button--primary";
    detailsLink.href = detailsUrl;
    detailsLink.textContent = "Zobacz szczegóły";

    footer.append(price, detailsLink);
    content.append(
        title,
        createTextElement("p", "car-card__subtitle", createCarSubtitle(car)),
        specifications,
        footer
    );
    article.append(media, content);

    return article;
}

export function getEnumLabel(value) {
    if (!value) {
        return "Brak danych";
    }

    return enumLabels[value]
        ?? value.replaceAll("_", " ").toLowerCase();
}

function createCarDetailsUrl(carId, options) {
    const query = new URLSearchParams({ id: String(carId) });

    if (options.startAt && options.endAt) {
        query.set("startAt", options.startAt);
        query.set("endAt", options.endAt);
    }

    return `/html/car-details.html?${query.toString()}`;
}

function createCarSubtitle(car) {
    return [
        car.productionYear,
        getEnumLabel(car.fuelType),
        formatEngineCapacity(car.engineCapacityCm3)
    ].filter(Boolean).join(" · ");
}

function createSpecification(label, value) {
    const item = document.createElement("div");
    item.className = "car-card__specification";
    item.append(
        createTextElement("dt", "car-card__specification-label", label),
        createTextElement("dd", "car-card__specification-value", value)
    );
    return item;
}

function createTextElement(tagName, className, text) {
    const element = document.createElement(tagName);
    if (className) {
        element.className = className;
    }
    element.textContent = text;
    return element;
}

function formatEngineCapacity(capacityCm3) {
    if (!capacityCm3) {
        return null;
    }

    return `${(capacityCm3 / 1000).toLocaleString("pl-PL", {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1
    })} l`;
}

function formatAcceleration(value) {
    return Number(value).toLocaleString("pl-PL", {
        minimumFractionDigits: 1,
        maximumFractionDigits: 2
    });
}
