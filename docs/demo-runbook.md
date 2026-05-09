# 🎯 RTDAS Demo Runbook

Step-by-step guide for demo day setup, execution, and troubleshooting.

---

## 1. Prerequisites

| Item | Minimum | Notes |
|------|---------|-------|
| Java | 17 LTS | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Network | Local Wi-Fi | All machines on same subnet |
| Firewall | Configured | Allow `java.exe` on private networks |

---

## 2. Server Setup

### Terminal Commands

```bash
# 1. Clone / open project
cd Real-Time-Distributed-Auction-System

# 2. Compile
mvn clean package -DskipTests

# 3. Start server (with explicit host for multi-NIC)
java -Djava.rmi.server.hostname=<YOUR_IP> \
     -cp target/rtdas-1.0.jar \
     com.auction.server.core.ServerLauncher
```

### What Happens on First Run

1. Creates directories: `data/`, `logs/`, `resources/images/`, `resources/thumbs/`
2. Creates `data/auction.db` with schema
3. Inserts default admin: `admin` / `admin`
4. Starts UDP broadcast on port 9999
5. Starts `AuctionReaper` thread

---

## 3. Client Connection

```bash
# On each client machine
cd Real-Time-Distributed-Auction-System
mvn javafx:run
```

### Connection Flow

1. **Connect Screen:**
   - Wait for server to appear in discovered list
   - Click server OR enter IP manually

2. **Login Screen:**
   - Username: `admin`
   - Password: `admin`

---

## 4. Seeding Demo Data

### Quick Seed Script

```bash
# Optional: run seeder to create demo users/auctions
mvn exec:java -Dexec.mainClass="com.auction.DemoSeeder"
```

Creates:
- 2 Sellers (`seller1`, `seller2`)
- 3 Bidders (`bidder1`, `bidder2`, `bidder3`)
- 5 sample auctions with placeholder images

### Manual Creation

If no seeder:
1. Login as Admin
2. Admin Panel → Create Users
3. Logout, login as Seller
4. Create Auction with test images

---

## 5. Demo Script

### Scene 1: Discovery & Login (2 min)

1. Show Connect screen auto-discovering server
2. Login as different roles to show role routing

### Scene 2: Gallery Browsing (2 min)

1. Filter by category
2. Sort by end time / current bid
3. Show low-quality image placeholder loading instantly

### Scene 3: Live Bidding (3 min)

1. Open same auction on 2+ clients
2. Place concurrent bids (show one wins, other gets stale error)
3. Highlight snipe protection (timer extends)
4. Show countdown colors (green → yellow → red)

### Scene 4: Seller Dashboard (2 min)

1. Sell items after winning bid
2. Cancel auction (only if zero bids)
3. Export CSV via file dialog

### Scene 5: Admin Panel (1 min)

1. View all users
2. Backup database
3. Check audit log

---

## 6. Troubleshooting

| Problem | Solution |
|---------|----------|
| Server not discovered | Check UDP port 9999 not blocked; use manual IP |
| RMI connection refused | Verify `-Djava.rmi.server.hostname` matches server IP |
| Login fails | Check default credentials `admin/admin`; reset DB if needed |
| Images not loading | Verify `resources/images/` exists; check file permissions |
| Timer inaccurate | Ensure server time sync; check client offset calculation |
| Bid rejected unexpectedly | Check stale price warning; refresh auction detail |

---

## 7. Reset Procedure

```bash
# Stop server
Ctrl+C

# Delete database to start fresh
rm data/auction.db

# Restart server
java -Djava.rmi.server.hostname=<YOUR_IP> \
     -cp target/rtdas-1.0.jar \
     com.auction.server.core.ServerLauncher
```

---

## 8. Expected Behavior Checklist

| Feature | Expected | Status |
|---------|----------|--------|
| UDP discovery | Server appears within 3s | ☐ |
| Login | Role-appropriate dashboard | ☐ |
| Gallery | Thumbnails load instantly | ☐ |
| Detail view | Full image loads after placeholder | ☐ |
| Place bid | Minimum 5% increment enforced | ☐ |
| Snipe protection | Timer extends, capped at 10 min | ☐ |
| Concurrent bids | Exactly one succeeds, other stale error | ☐ |
| Reaper | Auction auto-closes after end time | ☐ |
| Cancel | Only available on active with zero bids | ☐ |
| CSV export | File saves with correct columns | ☐ |
| Audit log | All actions recorded | ☐ |

---

## 9. Grading Alignment

| Requirement | Where Demonstrated |
|-------------|---------------------|
| OOP | User hierarchy, Serializable models |
| Collections | HashMap locks, List results |
| Multithreading | Reaper, polling threads, locks |
| File I/O | CSV export, image read/write |
| JDBC | All repositories |
| RMI | Full IAuctionService |
| Networking | UDP discovery |
| GUI | JavaFX with FXML/CSS |