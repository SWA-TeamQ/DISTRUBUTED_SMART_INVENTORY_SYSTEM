# Concurrency Model

## Server-Side
- Each RMI request executes on its own thread.
- Stock updates must be synchronized or guarded by a `ReentrantLock`.
- Use per-product locks to avoid blocking unrelated updates.

## Client-Side
- All RMI calls must run in `javafx.concurrent.Task`.
- UI updates must be on the JavaFX Application Thread via `Platform.runLater()`.

## Background Clock
- A client thread updates a dashboard clock every 1000ms.