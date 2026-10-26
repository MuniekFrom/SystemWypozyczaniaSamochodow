const carImages = {
    1: [
        "/images/cars/1/Toyota4Runner1.webp",
        "/images/cars/1/Toyota4Runner2.webp",
        "/images/cars/1/Toyota4Runner3.webp",
        "/images/cars/1/Toyota4Runner4.webp"
    ],

    2: [
        "/images/cars/2/SubaruForester1.webp",
        "/images/cars/2/SubaruForester2.webp",
        "/images/cars/2/SubaruForester3.webp",
        "/images/cars/2/SubaruForester4.webp"
    ],

    3: [
        "/images/cars/3/OpelMokka1.webp",
        "/images/cars/3/OpelMokka2.webp",
        "/images/cars/3/OpelMokka3.webp",
        "/images/cars/3/OpelMokka4.webp",
        "/images/cars/3/OpelMokka5.webp"
    ],

    4: [
        "/images/cars/4/AlfaRomeo1.webp",
        "/images/cars/4/AlfaRomeo2.webp",
        "/images/cars/4/AlfaRomeo3.webp",
        "/images/cars/4/AlfaRomeo4.webp",
        "/images/cars/4/AlfaRomeo5.webp",
        "/images/cars/4/AlfaRomeo6.webp"
    ],

    5: [
        "/images/cars/5/Peugeot1.webp",
        "/images/cars/5/Peugeot2.webp",
        "/images/cars/5/Peugeot3.webp"
    ]
};

export function getCarImages(carId) {
    return carImages[carId] ?? [];
}

export function getPrimaryCarImage(carId) {
    return getCarImages(carId)[0] ?? null;
}