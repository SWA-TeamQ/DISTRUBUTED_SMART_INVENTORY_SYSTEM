#!/bin/bash
# seed-demo-data.sh
# Convenience script to seed the database and generate test images
# Usage: ./seed-demo-data.sh

set -e

echo "🌱 RTDAS Demo Data Seeding"
echo "=========================="
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven and try again."
    exit 1
fi

echo "1️⃣  Running DemoSeeder..."
mvn exec:java -Dexec.mainClass=com.auction.server.core.DemoSeeder

echo ""
echo "2️⃣  Generating Test Images..."
mvn exec:java -Dexec.mainClass=com.auction.server.core.SeedTestImages

echo ""
echo "✅ Demo data setup complete!"
echo ""
echo "📋 Next steps:"
echo "   1. Start server: mvn exec:java"
echo "   2. Open client UI"
echo "   3. Login as bella-247 / pass123"
echo ""
echo "🔗 Test accounts:"
echo "   - bella-247 (bidder)"
echo "   - seller-alice, seller-bob, seller-charlie"
echo ""
echo "📖 See TESTING_GUIDE.md for detailed test scenarios"
