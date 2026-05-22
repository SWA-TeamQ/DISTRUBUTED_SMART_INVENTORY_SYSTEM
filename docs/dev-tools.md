Developer tools
===============

This file documents small developer utilities included to help validate RMI and DB behavior during development.

TestRegisterLogin
- Purpose: Programmatic RMI test that registers a short-lived test user and attempts login to validate end-to-end auth.
- How to run:

  1. Build the project:

     mvn -f "Real-Time-Distributed-Auction-System/pom.xml" -DskipTests package

  2. Run the tool:

     cd "Real-Time-Distributed Auction System\Real-Time-Distributed-Auction-System"
     java -cp target/classes;target/dependency/* com.auction.tools.TestRegisterLogin

  The tool prints registration/login results and the returned role.

QueryUsers
- Purpose: Local utility used to list users in a given SQLite file for quick verification when a SQLite CLI is not available.
- How to run: same build step as above, then run the class `com.auction.tools.QueryUsers` with the DB path as an argument.

Notes
- These tools are for developer/diagnostic use only and are not part of normal production operations.
