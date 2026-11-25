# Advanced Hash Table Techniques: Double Hashing vs. Linear Probing

A from-scratch Java implementation and empirical benchmark comparing **Double Hashing** against **Linear Probing** using open addressing.

## Features
- **Custom Open-Addressing Hash Tables:** Generic `LinearHashTable<K, V>` and `DoubleHashTable<K, V>` built without standard library map collections.
- **Collision & Deletion Handling:** Key-dependent secondary step sizing, custom hash bit-mixing, tombstone (`DELETED`) slot management, and dynamic prime-capacity rehashing.
- **Benchmarking Suite:** Evaluates average probe counts and operation latency (`ns/op`) across multiple load factors ($\alpha = 0.5, 0.8, 0.9$) and workloads (Random, Clustered, Mixed Insert/Lookup/Delete).

## Key Results
- **High Load Factor ($\alpha = 0.9$, Random Lookup):** Double hashing reduced average probes from **34.82** (Linear Probing) to **7.21**, cutting lookup latency by more than half (`169.4 ns/op` vs. `71.7 ns/op`).
- **Mixed Workload ($\alpha = 0.8$):** Double hashing achieved nearly **2x higher throughput** (`96.1 ns/op` vs. `181.4 ns/op`) with zero additional memory overhead.

## How to Run
```bash
javac HashTableProject.java
java HashTableProject
