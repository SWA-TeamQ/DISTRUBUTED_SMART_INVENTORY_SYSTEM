# 🎨 RTDAS UI/UX Design Specification

Comprehensive specification for every screen, component, interaction, and visual element.

---

## 1. Design Philosophy & "The Vibe"

| Principle | Detail |
|-----------|--------|
| **Design Language** | AtlantaFX PrimerDark |
| **Theme** | Deep dark mode (high contrast, refined grays, vibrant accent) |
| **Surface** | Card-based layouts with subtle borders and shadows |
| **Motion** | Subtle fade-ins, no heavy animations |

---

## 2. Visual Identity

### Colors (PrimerDark Based)

| Name | Hex | Usage |
|------|-----|-------|
| Background (Deep) | `#010409` | Primary window background |
| Surface (Cards) | `#0d1117` | Component background |
| Border | `#30363d` | Dividers and strokes |
| Primary Accent | `#58a6ff` | Buttons, active states, links |
| Success (Bids) | `#3fb950` | Highest bid indicators, success alerts |
| Danger (Expiring/Error) | `#f85149` | Countdown < 30s, errors, Close buttons |
| Warning | `#d29922` | Wait states, pending confirmations |
| Text (Primary) | `#c9d1d9` | Standard text |
| Text (Muted) | `#8b949e` | Secondary text, captions |

### Typography

| Element | Font | Size | Weight |
|---------|------|------|--------|
| H1 (Page Title) | Inter | 24px | Bold |
| H2 (Section) | Inter | 18px | Bold |
| Body | Inter | 14px | Regular |
| Small/Caption | Inter | 12px | Regular |
| Monospace (Prices/IDs) | JetBrains Mono | 14px | Regular |

---

## 3. Component Library

### 🗂️ Auction Card (Gallery View)

**Purpose:** Compact summary of an auction in the gallery grid.

| Element | Specification |
|---------|---------------|
| Header | Image container, **fixed 16:9** aspect ratio |
| Body - Title | Max 2 lines, bold, truncated with ellipsis |
| Body - Category | Pill-shaped badge, top-right of image |
| Body - Price | Large, monospace, Primary Accent color |
| Footer - Countdown | Real-time updating, color-coded (see §3.2) |
| Footer - Button | Ghost style "View Details" |

### ⏱️ Real-Time Countdown

| State | Threshold | Color | Behavior |
|-------|-----------|-------|----------|
| Normal | > 60 seconds | `#8b949e` (muted gray) | Standard display |
| Urgent | 30–60 seconds | `#d29922` (warning) | Yellow highlight |
| Critical | < 30 seconds | `#f85149` (pulsing red) | Pulsing animation |

### 🖼️ Image Gallery (LQIP System)

| Aspect | Specification |
|--------|---------------|
| Placeholder | 40×40 blurred thumbnail (stretched, GaussianBlur effect) |
| Transition | Cross-fade to full-res once loaded |
| Fallback | Material "Image Not Found" icon on `#30363d` background |
| Load trigger | Image 1 loads automatically; Images 2–3 on click |

---

## 4. Screen-by-Screen Breakdown

### 1. Connect Screen (The Gateway)

**Path:** `connect.fxml` → `ConnectController.java`

| Component | Requirement |
|-----------|-------------|
| Layout | Centered column, minimal |
| UDP Discovery List | `ListView` showing "Discovered Servers" with name + latency |
| Manual Entry | Expandable section with IP and port fields (`Integer` for port) |
| Logo | Large, non-functional decorative element |
| Auto-Connect Toggle | Optional: skip to last successful server |

**Must-Have Functionality:**
- Starts UDP listener on startup
- Refreshes list every 2s via broadcast
- Validates IP format before attempting connection
- Shows `Alert` on RMI unreachable (before login)

---

### 2. Login Screen

**Path:** `login.fxml` → `LoginController.java`

| Component | Requirement |
|-----------|-------------|
| Layout | Centered card, 350px wide |
| Username Field | `TextField` with @ icon prefix |
| Password Field | `PasswordField` with lock icon prefix |
| Action Button | Full-width, "Enter the Auction" in Primary Blue |
| Error Display | Generic "Invalid username or password" (same for either case) |

**Must-Have Functionality:**
- Calls `login(username, password)` → returns session token
- Stores token in `SessionManager`
- Routes to role-appropriate dashboard on success
- Clears fields on error, focuses username

---

### 3. Auction Gallery (The Marketplace)

**Path:** `gallery.fxml` → `GalleryController.java`

| Component | Requirement |
|-----------|-------------|
| Layout | Sidebar (240px) + Responsive Grid (main) |
| Sidebar - Search | Dropdown, NOT free-text (removed) |
| Sidebar - Category Filter | `CheckBox` list: Electronics, Furniture, Art, Other |
| Sidebar - Sort | `ComboBox`: End Time, Current Bid, Category |
| Grid | `FlowPane` or `GridPane`, responsive tiles |
| Auction Card | Per §3.1 |
| Polling | Every 2s via `ScheduledExecutorService` |
| Logout | Top-right button, calls `logout(token)` |

**Must-Have Functionality:**
- Click Auction Card → opens Detail view
- Filter + sort immediately affect visible list
- Polling updates countdown timers, bid amounts
- Cleanup polling thread on screen exit

---

### 4. Auction Detail View (High Intensity)

**Path:** `auction_detail.fxml` → `AuctionDetailController.java`

| Component | Requirement |
|-----------|-------------|
| Layout | Horizontal split: Image (left 50%), Bidding (right 50%) |
| Main Image | Full-res of image 1, loads in background after LQIP |
| Thumbnail Strip | 3 small thumbnails below main image |
| Current Winner | Avatar placeholder + username (or "No bids yet") |
| Current Bid | Large, monospace, updates live |
| Bid History | `TableView`: Timestamp | User | Amount (sorted desc) |
| Bid Input | `TextField` with placeholder "Min bid: starting + 5%" |
| Place Bid Button | Disabled during processing, shows `ProgressIndicator` |
| Countdown Timer | Color-coded per §3.2 |
| Snipe Toast | "Timer extended!" briefly on snipe protection trigger |

**Must-Have Functionality:**
- `placeBid()` uses `clientExpectedPrice` for stale detection
- Bid button disabled while RMI call in progress
- Images 2–3 load only when thumbnail clicked
- Countdown uses server time offset (via `serverTime()`)
- Displays `Alert` on stale price, self-bid, already-winning
- Polling stops automatically when view closed

---

### 5. User "My Activity" Dashboard

**Path:** `user_dashboard.fxml` → `UserDashboardController.java`

| Component | Requirement |
|-----------|-------------|
| Layout | TabPane with three tabs |
| Tab 1 - My Bids | Table: Auction | My Bid | Status | Time Left |
| Tab 2 - Won | Table: Auction | Final Price | End Time |
| Tab 3 - Outbid | Table: Auction | My Bid | Winning Bid |
| Polling | Every 2s, same as gallery |

**Must-Have Functionality:**
- Pulls from `getMyBids(token)`, `getMyWonAuctions(token)`
- Clicking row navigates to Auction Detail

---

### 6. User Auction Management Dashboard

**Path:** `user_dashboard.fxml` → `UserDashboardController.java`

| Component | Requirement |
|-----------|-------------|
| Layout | TabPane: Active, Sold, Expired |
| Stats Cards | Total Sales, Active Listings (top) |
| Auction Table | Per-status: Image | Title | Current Bid | Bids | Ends | Actions |
| FAB | "+" button, opens Create Auction dialog |
| Export CSV | Button per-tab, `FileChooser` save dialog |

**Must-Have Functionality:**
- `cancelAuction()` only if `status=ACTIVE` AND `bids=0`
- `relistAuction()` creates new row with `relisted_from` FK
- CSV export includes ALL statuses for the current user-owned auctions

---

### 7. Create/Edit Auction Dialog

**Path:** `create_auction_dialog.fxml` → `CreateAuctionController.java`

| Component | Requirement |
|-----------|-------------|
| Fields | Title, Description (TextArea), Category, Starting Price, End Time picker |
| Image Upload | 3 file pickers, client-side size check ≤2MB |
| Validation | End time must be future, price > 0 |
| Submit | Calls `createAuction(item, img1, img2, img3)` |

**Must-Have Functionality:**
- Server re-encodes all images to JPG, strips EXIF
- Generates 40×40 thumbnail from image 1
- Returns new auction ID on success

---

### 8. Admin Panel

**Path:** `admin_panel.fxml` → `AdminPanelController.java`

| Component | Requirement |
|-----------|-------------|
| User Table | All users with role and last-login |
| Create User Form | Username, password, role dropdown |
| Backup DB Button | Downloads `auction_backup.db` via `FileChooser` |
| Audit Log View | `TextArea` showing last N lines |
| Refresh Buttons | Per-section reload |

**Must-Have Functionality:**
- `createUser()` verifies admin token
- `backupDatabase()` uses SQLite online backup API
- All tabs are read-only except user creation

---

## 5. Interaction Patterns

| Pattern | Implementation Detail |
|---------|----------------------|
| Stale Price Feedback | Text field shakes, new price highlighted in red |
| Snipe Protection Alert | Toast "Timer Extended!" over countdown for 2s |
| Button Loading | Text replaced with `ProgressIndicator` during RMI |
| Empty State | Muted text "No auctions match your filters" |

---

## 6. Implementation Notes for Developers

- **Responsive Grid:** Use `FlowPane` with `prefWrapLength` for window resizing
- **Stylesheet:** `atlantafx.base.theme.PrimerDark` must be loaded in `Application.start()`
- **Accessibility:** Every interactive element gets `setAccessibleText()` and `getId()`
- **No external CSS:** Use AtlantaFX modifiers (elevated, bordered, etc.)

---

## 7. Out of Scope (UI)

- Animations beyond simple fade/cross-fade
- Free-text search in gallery
- Export preview before saving
- Custom themes