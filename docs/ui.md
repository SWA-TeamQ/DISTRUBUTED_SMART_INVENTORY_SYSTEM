# 🖼️ UI & Image Handling

This document covers the UI design choices and image loading optimizations in the Real-Time Distributed Auction System (RTDAS).

## 1. AtlantaFX Theme
We use **AtlantaFX (PrimerDark)** as the primary UI library. It provides high-quality components, smooth animations, and a cohesive, modern dark mode without the need for extensive custom CSS.

## 2. Image Handling (LQIP & Lazy Loading)
We support **up to 3 images per auction** (max 2 MB each). To keep the UI buttery‑smooth we use two tricks:

* **Thumbnail (LQIP – Low Quality Image Placeholder):** When a seller uploads the first image, the server automatically creates a tiny, blurry version (40×40 pixels, ~10 KB). This thumbnail loads instantly in gallery and detail views, so you never see a blank space.
* **Full‑Res Loading in Background:** The high‑quality image loads in a background thread and replaces the blurry placeholder when ready. Images 2 and 3 only load if you click on their small preview icons — saving bandwidth.
* **Missing Image Safety:** If an image file is deleted from the server, the client shows a default placeholder instead of crashing.

## 3. Screen Flow
1. **Connect Screen**: Listens for UDP broadcasts from active servers or allows manual IP entry.
2. **Login Screen**: Authenticates user and checks role.
3. **Role Dashboard**: Navigates to Admin Panel, Gallery (Bidder), or Seller Dashboard depending on the user's class.
