com.auction/
├── shared/                    # Shared between client & server (RMI transport)
│   ├── interfaces/
│   │   └── IAuctionService.java   # RMI contract (Remote interface)
│   ├── models/
│   │   ├── AuctionItem.java       # Serializable auction data
│   │   ├── Bid.java               # Serializable bid data
│   │   ├── User.java              # Base user class
│   │   └── Admin.java             # Admin subclass
│   ├── enums/
│   │   ├── AuctionStatus.java     # ACTIVE, SOLD, EXPIRED, CANCELLED
│   │   └── Category.java          # ELECTRONICS, FURNITURE, ART, OTHER
│   ├── exceptions/
│   │   ├── AuctionException.java          # Base exception
│   │   ├── InsufficientBidException.java  # < 5% increment
│   │   ├── SelfBidException.java          # Seller bidding on own item
│   │   ├── StaleDataException.java        # Price changed mid-bid
│   │   ├── DuplicateBidException.java     # Already highest bidder
│   │   ├── AuctionClosedException.java    # Auction ended/cancelled
│   │   ├── RateLimitedException.java      # Too many requests
│   │   ├── SnipeCapReachedException.java  # Max extension reached
│   │   └── UnauthorizedException.java     # Invalid session
│   └── Constants.java           # All magic values centralized
│
├── server/
│   ├── core/
│   │   ├── ServerLauncher.java      # main() entry point
│   │   ├── ServerBootstrap.java     # Component wiring
│   │   ├── SessionContext.java      # Record: (username, role)
│   │   ├── AuctionManager.java      # DEEP: All bidding logic
│   │   ├── LifecycleManager.java    # DEEP: State transitions
│   │   ├── LockManager.java         # Per-auction ReentrantLocks
│   │   ├── TransactionManager.java  # DB transaction boundaries
│   │   ├── ImageStore.java          # DEEP: Filesystem + DB sync
│   │   ├── UdpBroadcaster.java      # Server discovery broadcast
│   │   └── logging/
│   │       ├── AsyncLogger.java     # Producer-consumer queue
│   │       ├── LogEntry.java        # Log record
│   │       ├── LogCategory.java     # AUDIT, BID, SECURITY, SYSTEM
│   │       └── EventType.java       # Specific event types
│   ├── service/
│   │   ├── AuctionServiceImpl.java  # RMI Adapter layer
│   │   └── AuctionReaper.java       # Background lifecycle thread
│   ├── repository/
│   │   ├── DatabaseManager.java     # SQLite connection + schema
│   │   ├── UserRepository.java      # User CRUD
│   │   ├── AuctionRepository.java   # Auction CRUD
│   │   └── BidRepository.java       # Bid CRUD
│   └── util/
│       ├── SecurityUtil.java        # SHA-256 password hashing
│       └── AuditLogger.java         # (Legacy, replaced by AsyncLogger)
│
└── client/
    ├── ClientApp.java               # JavaFX Application entry
    ├── ClientLauncher.java          # Static main() wrapper
    ├── core/
    │   ├── ClientContext.java       # Singleton: holds session state
    │   └── ...
    ├── controllers/
    │   ├── ConnectController.java   # Server discovery UI
    │   ├── LoginController.java     # Authentication UI
    │   ├── SellerDashboardController.java
    │   ├── AdminPanelController.java
    │   ├── GalleryController.java   # (TODO: incomplete)
    │   ├── AuctionDetailController.java # (TODO: incomplete)
    │   └── RegistrationController.java
    ├── network/
    │   ├── RmiClientProvider.java   # RMI stub lookup
    │   └── UdpDiscoveryClient.java  # Listen for broadcasts
    ├── service/
    │   └── PollingService.java      # (TODO: 2s polling)
    └── ui/
        └── ViewLoader.java          # FXML navigation