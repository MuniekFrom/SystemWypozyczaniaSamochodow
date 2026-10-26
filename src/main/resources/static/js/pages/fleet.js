import { getCars } from "/js/core/api.js";
import { renderCarCards } from "/js/components/car-card.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { initPageLoader } from "/js/core/page-loader.js";

const filtersForm = document.querySelector("#fleet-filters");
const categoryFilter = document.querySelector("#category-filter");
const fuelFilter = document.querySelector("#fuel-filter");
const transmissionFilter = document.querySelector("#transmission-filter");
const sortFilter = document.querySelector("#sort-filter");
const clearFiltersButton = document.querySelector("#clear-filters");
const status = document.querySelector("#fleet-status");
const container = document.querySelector("#fleet-cars");

let allCars = [];

renderSiteHeader("fleet");
initPageLoader();
bindFleetEvents();
loadFleet();

function bindFleetEvents() {
    filtersForm?.addEventListener("change", renderFilteredFleet);

    clearFiltersButton?.addEventListener("click", () => {
        filtersForm?.reset();
        renderFilteredFleet();
    });
}

async function loadFleet() {
    setStatus("Ładowanie samochodów...");

    try {
        const cars = await getCars();

        allCars = cars.filter(
            car => !car.status || car.status === "ACTIVE"
        );

        renderFilteredFleet();
    } catch (error) {
        setError(error.message);
    }
}

function renderFilteredFleet() {
    if (!container) {
        return;
    }

    const selectedCategory = categoryFilter?.value ?? "";
    const selectedFuel = fuelFilter?.value ?? "";
    const selectedTransmission = transmissionFilter?.value ?? "";

    const filteredCars = allCars.filter(car =>
        (!selectedCategory || car.category === selectedCategory)
        && (!selectedFuel || car.fuelType === selectedFuel)
        && (!selectedTransmission || car.transmissionType === selectedTransmission)
    );

    const sortedCars = sortCars(filteredCars, sortFilter?.value ?? "default");
    renderCarCards(container, sortedCars);

    setStatus(
        sortedCars.length > 0
            ? `Znalezione samochody: ${sortedCars.length}.`
            : "Brak samochodów spełniających wybrane kryteria."
    );
}

function sortCars(cars, sortValue) {
    const sortedCars = [...cars];

    switch (sortValue) {
        case "price-asc":
            return sortedCars.sort(
                (first, second) => Number(first.dailyPrice) - Number(second.dailyPrice)
            );
        case "price-desc":
            return sortedCars.sort(
                (first, second) => Number(second.dailyPrice) - Number(first.dailyPrice)
            );
        case "year-desc":
            return sortedCars.sort(
                (first, second) => Number(second.productionYear) - Number(first.productionYear)
            );
        case "power-desc":
            return sortedCars.sort(
                (first, second) => Number(second.powerHp ?? 0) - Number(first.powerHp ?? 0)
            );
        default:
            return sortedCars.sort(
                (first, second) => Number(first.id) - Number(second.id)
            );
    }
}

function setStatus(message) {
    if (!status) {
        return;
    }

    status.textContent = message;
    status.classList.remove("fleet-content__status--error");
}

function setError(message) {
    if (!status) {
        return;
    }

    status.textContent = message;
    status.classList.add("fleet-content__status--error");
}
