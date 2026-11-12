# Temple ERP – GitHub Pages Landing Page

This folder contains a **static landing page** for GitHub Pages.

- **Purpose:** Present project overview, features, tech stack, and links.
- **Limitation:** GitHub Pages cannot run the full Temple ERP backend (Spring Boot, MongoDB, PDFs/email/QR server-side features).

## Deploy with GitHub Actions (recommended)
1. Go to repository **Settings → Pages**.
2. Under **Build and deployment**, set **Source** to **GitHub Actions**.
3. Open **Actions → “Deploy GitHub Pages (docs)”** and run the workflow (or push a change to `docs/**` on `main`).
4. Wait for the deployment to finish.

This uses `.github/workflows/pages.yml` to publish the `docs/` folder directly (no Jekyll build), which avoids Jekyll/SCSS build errors.

Expected URL: `https://suyog-shirsat2004.github.io/Temple-ERP-Java/`

## If you use “Deploy from a branch”
Select branch **main** and folder **/docs**. Keep `docs/.nojekyll` in place so the static files are served without Jekyll processing.

After deploying the actual backend app to a hosting platform, update the "Live App URL" text in `docs/index.html` with the verified working URL.
