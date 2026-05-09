# 🌐 RTDAS Networking & Discovery

Network protocols, discovery mechanism, and connection management.

---

## 1. UDP Broadcast Discovery

### Packet Format

```
RTDAS|v1|<rmiPort>|<serverName>|<serverId>|<rmiHost>
```

| Part | Example | Description |
|------|---------|-------------|
| Prefix | `RTDAS` | Static identifier |
| Version | `v1` | Protocol versioning |
| rmiPort | `1099` | RMI registry port |
| serverName | `AuctionServer-01` | Human-readable |
| serverId | `a1b2c3d4` | UUID for uniqueness |
| rmiHost | `192.168.1.100` | Preferred RMI host |

### Broadcast Parameters

| Parameter | Value |
|-----------|-------|
| Port | `9999` (UDP) |
| Interval | Every 2000ms |
| Interface | All non-loopback IPv4 interfaces |

### Client Discovery Flow

1. `ConnectController` opens `DatagramSocket` on port 9999
2. Listens for broadcasts, parses packets
3. Extracts `rmiHost` from payload (or uses packet source IP)
4. Displays in `ListView` with server name and latency estimate

### Why UDP?

- Server discovery is inherently broadcast
- No persistent connection needed
- Works on local Wi-Fi without configuration
- Demonstrates networking beyond RMI

---

## 2. Manual Connection

| Field | Validation |
|-------|------------|
| IP Address | IPv4 format check |
| Port | Integer, typically 1099 |
| Button | "Connect" attempts RMI handshake |

**Error Handling:**
- Invalid format: Inline warning
- RMI unreachable: `Alert` with "Server not reachable"
- Success: Proceed to Login screen

---

## 3. RMI Communication Pattern

### Chosen: Polling (not Push)

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Push/Callbacks | Rejected | Complexity, firewall issues, client-side RMI export |
| Polling | Accepted | Simple, reliable, fits demo scale |

### Polling Cadence

- **Interval:** 2000ms (every 2 seconds)
- **Scope:** Gallery and Auction Detail views
- **Cleanup:** Stop polling when view is closed (`shutdown()` in controller)

### Stale Price Detection

```java
// Client holds expected price when rendering
long expectedPriceCents = item.getCurrentBidCents();

// On bid, send expected to server
placeBid(auctionId, token, amountCents, expectedPriceCents);

// Server validates and rejects if stale
if (currentPrice != expectedPrice) throw new StaleDataException();
```

---

## 4. Clock Synchronization

### Server Time Endpoint

```java
String serverTime(); // Returns ISO-8601 UTC, e.g., "2026-05-09T22:40:00Z"
```

### Client Offset Calculation

```java
long offsetMs = serverTimeMillis - System.currentTimeMillis();
// All countdowns use: serverNow = clientNow + offsetMs
```

**Purpose:** Accurate countdown timers despite client clock drift.

---

## 5. Reconnection & Error Handling

### Connection Lost Detection

- **Polling failures:** Track consecutive failures
- **Threshold:** 3 failed polls in a row
- **UI:** Show "Connection lost" banner over content

### Recovery Options

1. **Auto-retry:** Background retry every 5s
2. **Manual reconnect:** Button to return to Connect screen
3. **Choose another server:** If multiple discovered

### Last Server Persistence

- Store last successful server in `~/.rtdas/last_server.json`
- Pre-fill Connect screen on next launch

---

## 6. Multi-NIC Configuration

### Problem

A server with multiple network interfaces may bind RMI to the wrong IP.

### Solution

Start server with explicit host binding:

```bash
java -Djava.rmi.server.hostname=192.168.1.100 \
     -jar rtdas-server.jar
```

### Documentation

This flag is documented in `docs/demo-runbook.md` for demo day setup.

---

## 7. Firewall & Network Considerations

| Issue | Resolution |
|-------|------------|
| Windows Defender Firewall | Allow `java.exe` on private networks |
| UDP broadcast blocked | Manual entry always available |
| Wi-Fi multicast filtering | Use broadcast, not multicast |

---

## 8. Message Flow Diagram

```
Client                              Server
  |                                    |
  |--- UDP Discover ------------------>| (every 2s)
  |                                    |
  |<-- RTDAS|v1|1099|Name|ID|IP --------|
  |                                    |
  |--- RMI Connect -------------------->|
  |                                    |
  |--- login() ----------------------->|
  |                                    |
  |<-- Session token -------------------|
  |                                    |
  |--- getActiveAuctions() ----------->| (poll every 2s)
  |                                    |
  |<-- List<AuctionItem> --------------|
  |                                    |
  |--- placeBid() -------------------->|
  |                                    |
  |<-- (success or exception) ---------|
```

---

## 9. Out of Scope

- Internet routing / NAT traversal
- SSL/TLS encryption
- DNS-based discovery
- Peer-to-peer fallback