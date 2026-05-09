# 🖼️ RTDAS UI Guide

This document provides a concise overview. For full UI/UX specification, see `docs/DESIGN.md`.

---

## Quick Reference

| Screen | Controller | Roles |
|--------|------------|-------|
| `connect.fxml` | `ConnectController` | All (pre-login) |
| `login.fxml` | `LoginController` | All |
| `gallery.fxml` | `GalleryController` | Bidder, Seller |
| `auction_detail.fxml` | `AuctionDetailController` | Bidder, Seller |
| `bidder_dashboard.fxml` | `BidderDashboardController` | Bidder |
| `seller_dashboard.fxml` | `SellerDashboardController` | Seller |
| `admin_panel.fxml` | `AdminPanelController` | Admin |

## Key Interactions

- **Polling:** 2-second interval for gallery and detail views
- **Bid flow:** Click "Place Bid" → validates → disables button → Task → success/error
- **Image loading:** LQIP placeholder → background full-image load → cross-fade

## Theme

AtlantaFX `PrimerDark` theme. See `docs/DESIGN.md#Visual-Identity` for colors.