# 📚 RTDAS Documentation Index

Master index of all documentation. Click any link to jump to that section.

---

## README.md

- [Quick Start](#quick-start) — Server and client launch commands
- [Documentation Overview](#documentation) — Links to all doc files
- [Tech Stack](#tech-stack) — Technologies used

---

## docs/RTDAS_PRD.md

### 1. Problem & Solution
- [Problem Statement](#problem-statement) — Why this project exists
- [Solution Summary](#solution) — English auction platform overview

### 2. User Stories
- [Server Discovery & Connection (1-5)](#server-discovery--connection)
- [Authentication & User Management (6-12)](#authentication--user-management)
- [Auction Browsing & Discovery (13-19)](#auction-browsing--discovery)
- [Bidding (20-28)](#bidding)
- [Snipe Protection with Cap (29-30)](#snipe-protection-with-cap)
- [Bidder Activity Dashboard (31)](#bidder-activity-dashboard)
- [Auction Creation & Management (32-41)](#auction-creation--management-seller)
- [Administration (42-44)](#administration)
- [Auction Lifecycle (45-47)](#auction-lifecycle--state-machine)
- [Data Export & Audit (48-49)](#data-export--audit)
- [Robustness & Edge Cases (50-54)](#robustness--edge-cases)

### 3. Implementation Decisions
- [1. Build System](#1-build-system--single-module-maven-option-a)
- [2. Networking](#2-networking--java-rmi--udp-discovery)
- [3. RMI Interface Contract](#3-rmi-interface-contract)
- [4. Database](#4-database--sqlite-via-jdbc)
- [5. Currency Model](#5-concurrency-model)
- [6. Concurrency Model](#6-concurrency-model)
- [7. Snipe Protection with Cap](#7-snipe-protection-with-cap)
- [8. Image Handling](#8-image-handling--lqip-pattern)
- [9. Security](#9-security)
- [10. Auction State Machine](#10-auction-state-machine)
- [11. Audit Log Format](#11-audit-log-format)
- [12. JavaFX Views](#12-javafx-views--application-flow)
- [13. Categories](#13-categories)

### 4. Testing
- [What makes a good test](#what-makes-a-good-test)
- [Modules to test](#modules-to-test)

### 5. Out of Scope
- [12 items explicitly excluded](#out-of-scope)

### 6. Demo & Grading Notes
- [Cross-References](#cross-references)

---

## docs/DESIGN.md

### 1. Design Philosophy
- Professional, dark-mode native

### 2. Visual Identity
- [Color palette](#colors-primerdark-based)
- [Typography scale](#typography)

### 3. Component Library
- [Auction Card](#-auction-card-gallery-view) — Layout, elements
- [Real-Time Countdown](#-real-time-countdown) — Color states
- [Image Gallery (LQIP)](#-image-gallery-lqip-system) — Placeholder, transition

### 4. Screen-by-Screen Breakdown
- [Connect Screen](#1-connect-screen-the-gateway)
- [Login Screen](#2-login-screen)
- [Bidder Gallery](#3-bidder-gallery-the-marketplace)
- [Auction Detail View](#4-auction-detail-view-high-intensity)
- [Bidder Dashboard](#5-bidder-dashboard)
- [Seller Dashboard](#6-seller-dashboard)
- [Create/Edit Auction](#7-createedit-auction-dialog)
- [Admin Panel](#8-admin-panel)

### 5. Interaction Patterns
- Stale price feedback, snipe alert, loading states

### 6. Implementation Notes
- Responsive grids, AtlantaFX setup

---

## docs/architecture.md

### 1. The Core Concept
- English auction explanation

### 2. Key Terms
- Players: Server, Client, RMI, Admin, Seller, Bidder

### 3. Auction Rules
- Minimum increment, snipe protection, self-bid prevention

### 4. Auction Lifecycle
- [State machine](#4-auction-lifecycle-state-machine)

### 5. The Auction Reaper
- Background thread that closes auctions

### 6. RMI Interface Contract
- Session tokens, method signatures

### 7. Concurrency Model
- Per-auction locks, atomic transactions

### 8. Clock Synchronization
- Server time offset for accurate countdowns

---

## docs/database.md

### 1. Database Overview
- SQLite engine, repository pattern

### 2. Schema Definition
- [users table](#users-table)
- [auction_items table](#auctionitems-table)
- [bids table](#bids-table)

### 3. Indexes
- Query optimization rationale

### 4. Data Types: Why Cents?
- Integer vs floating-point rationale

### 5. Transactions
- Bid commit atomicity

### 6. Backup Strategy
- SQLite online backup API

### 7. CSV Export
- Column specification, RFC 4180 escaping

### 8. Security
- Password hashing, admin-only registration

---

## docs/networking.md

### 1. UDP Broadcast Discovery
- [Packet format](#packet-format)
- Broadcast parameters
- Client discovery flow

### 2. Manual Connection
- Validation and error handling

### 3. RMI Communication Pattern
- Polling vs push
- Stale price detection

### 4. Clock Synchronization
- Server time endpoint and offset

### 5. Reconnection & Error Handling
- Connection lost detection, recovery options

---

## docs/demo-runbook.md

### 1. Prerequisites
- Java, Maven, network requirements

### 2. Server Setup
- Terminal commands, first-run behavior

### 3. Client Connection
- Connection flow

### 4. Seeding Demo Data
- Seed script or manual creation

### 5. Demo Script
- 5 scenes with estimated timing

### 6. Troubleshooting
- Common problems and solutions

### 7. Reset Procedure
- Clean database reset

### 8. Expected Behavior Checklist
- Feature verification table

---

## Quick Reference Table

| Topic | File | Section |
|-------|------|---------|
| User Stories | RTDAS_PRD.md | Line ~23 |
| State Machine | architecture.md | §4 |
| Database Schema | database.md | §2 |
| RMI Contract | RTDAS_PRD.md | §3 |
| Session Tokens | architecture.md | §6 |
| UI Screens | DESIGN.md | §4 |
| Concurrency Rules | architecture.md | §7 |
| Demo Commands | demo-runbook.md | §2 |