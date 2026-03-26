# Overview

## Purpose
This document summarizes the Distributed Smart Inventory Management System (DSIMS), its goals, and what the project will deliver.

## Goals
- Provide a distributed, multi-user inventory system.
- Demonstrate advanced Java skills: OOP, collections, multithreading, Java I/O, JDBC, and RMI.
- Ensure role-based access and concurrency-safe stock updates.
- Offer a JavaFX desktop client for Admin, Manager, and Staff roles.

## Scope
DSIMS consists of:
- **Headless RMI Server:** business logic, synchronization, persistence.
- **JavaFX Desktop Client:** UI for inventory operations.
- **Hybrid Persistence:** relational database + file streams (CSV/logs).

## Key Features
- Role-based permissions (Admin, Manager, Staff)
- Inventory browsing and stock updates
- CSV import/export
- Audit logging
- Thread-safe server operations