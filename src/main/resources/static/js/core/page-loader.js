export function initPageLoader() {
    const loader = document.querySelector("#page-loader");
    const pageContent = document.querySelector(".page-content");

    if (!loader) {
        document.body.classList.remove("is-loading");
        document.body.classList.add("is-ready");
        pageContent?.removeAttribute("inert");
        return;
    }

    if (loader.dataset.initialized === "true") return;
    loader.dataset.initialized = "true";

    const reducedMotion = window.matchMedia(
        "(prefers-reduced-motion: reduce)"
    );

    let finished = false;
    let revealTimer;
    let imageTimer;
    let exitDuration = 700;
    const introFrames = [];

    pageContent?.setAttribute("inert", "");

    function showPage() {
        if (finished) return;
        finished = true;

        window.clearTimeout(revealTimer);
        window.clearTimeout(imageTimer);
        introFrames.forEach((frame) => window.cancelAnimationFrame(frame));
        document.removeEventListener("keydown", handleKeydown);
        window.removeEventListener("pagehide", showPage);
        reducedMotion.removeEventListener("change", handleMotionChange);

        const skippedWithKeyboard = loader.contains(document.activeElement);

        document.body.classList.remove("is-loading");
        document.body.classList.add("is-ready");
        pageContent?.removeAttribute("inert");

        loader.classList.add("page-loader--hidden");
        loader.setAttribute("aria-hidden", "true");
        loader.setAttribute("inert", "");

        if (skippedWithKeyboard) {
            const main = document.querySelector("main");
            main?.setAttribute("tabindex", "-1");
            main?.focus({ preventScroll: true });
        }

        window.setTimeout(() => loader.remove(), reducedMotion.matches ? 0 : exitDuration + 50);
    }

    function handleKeydown(event) {
        if (event.key === "Escape") showPage();
    }

    function handleMotionChange(event) {
        if (event.matches) showPage();
    }

    loader.querySelector(".headlight-intro__skip")
        ?.addEventListener("click", showPage);

    document.addEventListener("keydown", handleKeydown);
    window.addEventListener("pagehide", showPage);
    reducedMotion.addEventListener("change", handleMotionChange);

    async function startIntro() {
        if (reducedMotion.matches || !loader.dataset.loaderImage) {
            showPage();
            return;
        }

        const photo = new Image();
        photo.src = loader.dataset.loaderImage;

                imageTimer = window.setTimeout(showPage, 2000);

        try {
            await photo.decode();
            window.clearTimeout(imageTimer);
            if (finished) return;

            const styles = getComputedStyle(loader);
            const sceneDuration = Number.parseFloat(
                styles.getPropertyValue("--intro-scene-duration")
            ) || 2400;
            exitDuration = Number.parseFloat(
                styles.getPropertyValue("--intro-exit-duration")
            ) || 700;

            // Dajemy przeglądarce czas na przygotowanie warstw.
            introFrames.push(window.requestAnimationFrame(() => {
                if (finished) return;
                introFrames.push(window.requestAnimationFrame(() => {
                    if (finished) return;
                    loader.classList.add("is-playing");
                    revealTimer = window.setTimeout(showPage, sceneDuration);
                }));
            }));
        } catch {
            showPage();
        }
    }

    startIntro();
}
