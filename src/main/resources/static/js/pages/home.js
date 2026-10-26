import { getCars } from "/js/core/api.js";
import { renderCarCards } from "/js/components/car-card.js";
import { renderSiteHeader } from "/js/components/site-header.js";
import { initPageLoader } from "/js/core/page-loader.js";

renderSiteHeader();
initPageLoader();
loadFeaturedCars();

async function loadFeaturedCars() {
    const status = document.querySelector("#featured-status");
    const container = document.querySelector("#featured-cars");

    if (!status || !container) {
        return;
    }

    try {
        const cars = await getCars();
        const featuredCars = cars
            .filter(car => !car.status || car.status === "ACTIVE")
            .slice(0, 3);

        renderCarCards(container, featuredCars);

        status.textContent = featuredCars.length > 0
            ? `Wybrane samochody: ${featuredCars.length}.`
            : "Obecnie nie ma aktywnych samochodów.";
    } catch (error) {
        status.textContent = error.message;
        status.classList.add("home-featured__status--error");
    }
}
