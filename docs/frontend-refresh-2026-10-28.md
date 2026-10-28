# Shopping-Web frontend refresh — 28 October 2026

Live site: https://shop.imrocky.com/

## Delivered

- Rebuilt storefront, category and product pages, with photography, search, sorting, stock filters, related products, and useful empty/loading/error states.
- Added a click-to-open shopping bag with keyboard-friendly drawer, quantity controls, price totals, voucher feedback, and sign-in return to checkout. Cart persists as product IDs and quantities, and restores current prices and availability from the existing API.
- Refreshed sign-in, registration, password change, customer orders/devices, and all four administrator pages. Existing Spring Security, CSRF, session, Stripe and order APIs remain in use.
- Imported 8 clearly identified demo products into the existing database through the authenticated admin interface. Categories: Audio & Tech (3), Everyday Carry (3), Desk & Living (2). Initial available units: 125. Import skips matching existing category/product pairs and never overwrites their price or stock.
- Replaced broken-image fallback, updated branding/favicon, and added responsive layouts, visible keyboard focus, form labels and reduced-motion support.

## Validation performed

- ESLint and TypeScript checks passed.
- Next.js production webpack build passed locally and on the Lightsail host.
- All existing route families served successfully; dynamic category and all eight product pages returned their expected content. Unknown URLs render the custom not-found screen (Next.js streaming can return HTTP 200 for streamed not-found content).
- All 16 uploaded thumbnail/large image URLs returned image responses. Storefront cards use large images for sharp rendering.
- Public API returned eight distinct products across three categories, all with positive prices and stock; category filtering matched product category IDs.
- Browser checks: product import, safe repeated import, category navigation, search, low-to-high price sort, add-to-bag confirmation, quantity increment and totals, invalid-voucher error, cart persistence through reload, customer orders/devices, admin products/categories/orders/sessions. Temporary bag contents were removed afterwards.
- Desktop and 390 px mobile layouts were visually inspected; mobile bag opening was checked. Browser extension console errors were unrelated to the app.
- Unauthenticated access to the admin orders API was rejected with HTTP 401. No account/password changes, session revocations or payment charges were performed. A completed Stripe payment/webhook cycle was not tested in this refresh.

## Deployment and recovery

Only Shopping-Web frontend source/build files were replaced and the existing shop-web PM2 process restarted. The Java service stayed active. Nginx, PM2 configuration, systemd, shared MySQL configuration, environment files and all other projects were left as they were. No secrets or private keys were inspected or included in release archives.

Original frontend build/source backup on the server:

`/home/ubuntu/apps/shopping/frontend-backup-20261028-everyday`

An additional backup of the first refreshed version exists at:

`/home/ubuntu/apps/shopping/frontend-backup-20261028-before-image-refinement`

For a frontend rollback, stop shop-web, preserve the active replaced source/build paths, restore those paths from the original backup into `/home/ubuntu/apps/shopping/frontend`, and restart shop-web. Keep the current `node_modules` and protected environment files in place. Frontend rollback does not remove catalog records or uploads.

## Preview

!Live desktop storefront

!Live product collection
