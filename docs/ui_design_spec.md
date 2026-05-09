# 🎨 UI/UX Design Specification: RTDAS

This document provides a comprehensive design specification for the **Real-Time Distributed Auction System (RTDAS)**. It defines the visual identity, component architecture, and interaction models required to build a premium, cohesive user experience.

> [!TIP]
> This document is optimized to be used as a reference for design tools like **Stitch** or for manual JavaFX implementation using **AtlantaFX**.

---

## 1. Design Philosophy & "The Vibe"

The RTDAS UI should feel **professional, high-performance, and "Dark-Mode Native."** Since it deals with real-time auctions, the interface must prioritize speed, clarity of information (especially timers and prices), and smooth transitions.

*   **Design Language:** GitHub Primer (via AtlantaFX PrimerDark).
*   **Theme:** Deep Dark Mode (High contrast, refined grays, vibrant accent colors).
*   **Surface:** Card-based layouts with subtle borders and shadows to create depth.
*   **Motion:** Subtle fade-ins for images, pulsing effects for expiring auctions, and smooth list transitions.

---

## 2. Visual Identity (Design Tokens)

### 🎨 Color Palette (PrimerDark Based)
*   **Background (Deep):** `#010409` (Primary window background)
*   **Surface (Cards/Modals):** `#0d1117` (Component background)
*   **Border:** `#30363d` (Subtle dividers and component strokes)
*   **Primary Accent:** `#58a6ff` (Buttons, active states, links)
*   **Success (Bids):** `#3fb950` (Highest bid indicators, success alerts)
*   **Danger (Expiring/Error):** `#f85149` (Countdown < 30s, errors, "Close" buttons)
*   **Warning:** `#d29922` (Wait states, pending confirmations)
*   **Text (Primary):** `#c9d1d9`
*   **Text (Secondary/Muted):** `#8b949e`

### 🔡 Typography
*   **Font Family:** Inter (System fallback: Segoe UI, San Francisco)
*   **Headings:** Bold, high tracking, clear hierarchy.
*   **Monospace:** JetBrains Mono (Used for Auction IDs and Price amounts for perfect alignment).
*   **Scale:**
    *   `h1`: 24px (Page Titles)
    *   `h2`: 18px (Section Headers)
    *   `body`: 14px (Standard text)
    *   `small`: 12px (Captions, timestamps)

---

## 3. Component Library

### 🗂️ Auction Card (Gallery View)
*   **Header:** Image container (Fixed 16:9 aspect ratio).
*   **Body:**
    *   Bold Title (max 2 lines).
    *   Category Badge (Small, pill-shaped).
    *   Current Price (Large, monospace, Primary Accent color).
*   **Footer:**
    *   Countdown Timer (Updates in real-time).
    *   "View Details" Button (Ghost style).

### ⏱️ Real-Time Countdown
*   **Normal (>1 min):** Muted Gray.
*   **Urgent (<1 min):** Warning Yellow.
*   **Critical (<30s):** Pulsing Danger Red.

### 🖼️ Image Gallery (LQIP System)
*   **Placeholder:** 40x40 blurred thumbnail (LQIP) stretched to fit, using a `GaussianBlur` effect.
*   **Transition:** Cross-fade to full-res image once loaded.
*   **Fallback:** Material-design "Image Not Found" icon in `#30363d` background.

---

## 4. Screen-by-Screen Breakdown

### 1. Connect Screen (The Gateway)
*   **Layout:** Minimalist, centered column.
*   **Feature:** A `ListView` showing "Discovered Servers" with names and latencies.
*   **Manual Entry:** "Expandable" section for Manual IP/Port entry.
*   **Visual:** Large, glowing RTDAS Logo.

### 2. Login Screen
*   **Layout:** Centered Card (350px width).
*   **Fields:** Standard Username/Password with icon prefixes.
*   **Action:** Large "Enter the Auction" button (Primary Blue).

### 3. Bidder Gallery (The Marketplace)
*   **Layout:** Sidebar (Left) + Grid (Right).
*   **Sidebar:** Search bar, Category checkboxes, Sort dropdown.
*   **Grid:** Responsive tiles of Auction Cards.

### 5. Auction Detail View (High Intensity)
*   **Layout:** Split screen (Left: Image Gallery, Right: Bidding Controls).
*   **Gallery:** Large main image + 3 small selectable thumbnails below.
*   **Bidding Section:**
    *   Current Winner Display (User Avatar + Username).
    *   Bid History Table (Timestamp | User | Amount).
    *   Input Field: "Place Bid (Min Increment: +5%)".
    *   Action: Heavyweight "PLACE BID" button.

### 6. Seller Dashboard
*   **Layout:** Tabbed View (Active, Sold, Expired).
*   **Stats:** Summary cards at the top (Total Sales, Active Listings).
*   **Action:** Fixed "Floating Action Button" (FAB) for "Create New Auction."

---

## 5. Interaction Patterns

1.  **Stale Price Feedback:** If a user bids on an old price, the input field should shake (Error animation) and highlight the new price in red.
2.  **Snipe Protection Alert:** When a bid extends the timer, a toast notification or "Timer Extended!" badge should briefly appear over the countdown.
3.  **Loading States:** Buttons should replace text with a `ProgressIndicator` during RMI calls.
4.  **Empty States:** Clear illustrations and "Try changing filters" text when no auctions match criteria.

---

## 6. Implementation Notes for Stitch

When generating UI components, ensure:
*   **Responsive Grids:** Use `FlowPane` or `GridPane` for the gallery.
*   **Cohesive Styles:** All components should inherit from the `atlantafx.base.theme.PrimerDark` stylesheet.
*   **Accessibility:** Every interactive element must have a `Unique ID` (e.g., `btn-place-bid`, `txt-search-auctions`).
