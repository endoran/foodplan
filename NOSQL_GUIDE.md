# NoSQL Database Types: A Practical Guide for FoodPlan

## The Five Families of NoSQL

### 1. Document Stores
**Examples:** MongoDB, CouchDB, Amazon DocumentDB

**How they work:** Store data as self-contained JSON/BSON documents. Each document can have a different structure. Documents are grouped into "collections" (like tables, but flexible).

**Great for:**
- Data that naturally nests (a recipe containing its ingredients)
- Rapid prototyping where the schema evolves
- Content management, catalogs, user profiles

**Not great for:**
- Highly relational data with lots of joins
- Transactions spanning many documents (though MongoDB has added multi-document transactions)
- Data where relationships between entities are more important than the entities themselves

**Example — a recipe as a document:**
```json
{
  "_id": "rec_001",
  "name": "Tacos",
  "instructions": "Brown the beef, warm the shells...",
  "ingredients": [
    { "name": "Ground Beef", "quantity": 1, "unit": "LBS", "groceryCategory": "MEAT" },
    { "name": "Cheddar", "quantity": 2, "unit": "CUP", "groceryCategory": "DAIRY" }
  ]
}
```

---

### 2. Key-Value Stores
**Examples:** Redis, Amazon DynamoDB, Memcached, etcd

**How they work:** The simplest model — every piece of data is stored as a key-value pair. Think of it as a giant distributed HashMap. The database doesn't know or care what's inside the value.

**Great for:**
- Caching (session data, computed results)
- Shopping carts, user preferences
- Real-time leaderboards, counters
- Anything where you always look up by a known key

**Not great for:**
- Querying by attributes inside the value (you can only look up by key)
- Complex relationships
- Data you need to search, filter, or aggregate

**Example:**
```
Key: "user:42:cart"  →  Value: { items: [...] }
Key: "session:abc"   →  Value: { userId: 42, expires: ... }
```

---

### 3. Column-Family (Wide-Column) Stores
**Examples:** Apache Cassandra, HBase, ScyllaDB

**How they work:** Data is stored in rows and columns, but unlike relational DBs, each row can have different columns. Columns are grouped into "families." Optimized for writing and reading huge volumes of data across many nodes.

**Great for:**
- Time-series data (IoT sensor readings, event logs)
- Very high write throughput at massive scale
- Data naturally partitioned by time or geography
- Analytics on huge datasets

**Not great for:**
- Ad-hoc queries (you must design your table around your query patterns)
- Small-scale applications
- Data that needs frequent updates to individual fields
- Anything where you don't know your query patterns upfront

**Example — sensor data:**
```
Row Key: "sensor_42:2024-01-15"
  Columns: { temp_08:00: 72.1, temp_08:05: 72.3, humidity_08:00: 45%, ... }
```

---

### 4. Graph Databases
**Examples:** Neo4j, Amazon Neptune, ArangoDB, JanusGraph

**How they work:** Store data as nodes (entities) and edges (relationships). Edges are first-class citizens with their own properties. Traversing relationships is O(1), not a join.

**Great for:**
- Social networks (who knows whom)
- Recommendation engines ("people who bought X also bought Y")
- Fraud detection (finding suspicious relationship patterns)
- Knowledge graphs, dependency trees

**Not great for:**
- Simple CRUD with minimal relationships
- High-volume writes
- Data that's naturally tabular or document-shaped
- Full-text search

**Example — recipe relationships:**
```
(Pete)-[:LIKES]->(Tacos)
(Tacos)-[:USES {quantity: 2, unit: "CUP"}]->(Cheddar)
(Cheddar)-[:IN_AISLE]->(Dairy)
(Tacos)-[:PAIRS_WITH]->(Margaritas)
```

---

### 5. Time-Series Databases
**Examples:** InfluxDB, TimescaleDB, Amazon Timestream

**How they work:** Optimized specifically for timestamped data. Automatically handle data retention, downsampling, and time-windowed queries.

**Great for:**
- Monitoring and metrics (CPU usage, request latency)
- Financial market data
- IoT sensor streams

**Not great for:**
- General-purpose application data
- Anything without a strong time component

---

## Head-to-Head Comparison

| Criteria | Document | Key-Value | Column-Family | Graph | Time-Series |
|----------|----------|-----------|---------------|-------|-------------|
| Schema flexibility | High | N/A | Medium | Medium | Low |
| Query complexity | Medium | Key only | Pre-planned | Traversals | Time-windowed |
| Relationships | Embedded/ref | None | Denormalized | Native | None |
| Scale-out | Good | Excellent | Excellent | Moderate | Good |
| Learning curve | Low | Very low | High | Medium | Low |
| Best at | Flexible docs | Speed/cache | Massive writes | Connections | Timestamped |

---

## So What's Right for FoodPlan?

Let's evaluate against what this app actually needs:

### The data model
- **Recipes** with nested ingredients and measurements
- **Ingredients** with categorizations (grocery aisle, storage type)
- **Meal plans** (future: weekly plan → recipes → aggregated shopping list)
- Modest scale — this is a personal/family tool, not Netflix

### What matters
1. **Natural nesting** — a recipe IS a document with embedded ingredients
2. **Flexible schema** — recipes vary wildly (some have prep time, some have photos, some have nutritional info)
3. **Simple queries** — find recipes by name, filter ingredients by category, aggregate a shopping list
4. **Easy to develop** — minimal ops overhead for a personal project

### What doesn't matter
1. Massive scale or write throughput
2. Complex graph traversals
3. Time-series analysis
4. Sub-millisecond caching

### The verdict

**MongoDB is actually a solid choice here.** Your instinct was right even if you called it uneducated.

Here's why:

- A recipe is a **textbook document store use case** — self-contained, naturally nested, variable structure
- The grocery-category and storage-category groupings are simple filters, not deep relationship traversals (so graph is overkill)
- Spring Data MongoDB is mature, well-documented, and already in the stack
- Schema flexibility means you can add fields (prep time, servings, tags, photos) without migrations
- MongoDB's aggregation pipeline is powerful enough to build a shopping list aggregator later
- For a personal project, MongoDB's free tier on Atlas or a local Docker container is zero-cost ops

**The one case where you might reconsider:** If the app evolves to focus heavily on "suggest recipes based on what I have in my pantry" or "what goes well with X" — that's relationship-heavy territory where a graph DB would shine. But you can cross that bridge if you get there; starting with Mongo doesn't lock you out.

### Runner-up

**PostgreSQL with JSONB** — yes, it's relational, but Postgres's JSONB support gives you document-store flexibility with the safety net of SQL when you want it. Spring Data JPA is arguably even simpler than Spring Data MongoDB. Worth considering if you want the best of both worlds. But for a pure recipe manager, Mongo keeps things simpler.

---

## TL;DR

| DB Type | FoodPlan Fit | Why |
|---------|-------------|-----|
| Document (MongoDB) | **Best fit** | Recipes are documents. Period. |
| Key-Value (Redis) | Nope | No query flexibility |
| Column-Family (Cassandra) | Overkill | We're not Netflix |
| Graph (Neo4j) | Future maybe | Only if heavy recommendation features |
| Time-Series (InfluxDB) | Nope | No time-series data |

**Recommendation: Stick with MongoDB.** It was the right call.
