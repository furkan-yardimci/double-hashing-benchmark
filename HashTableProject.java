import java.util.Random;

public class HashTableProject {

    private static final Object DELETED = new Object();

    private static int mixHash(int hash) {
        hash ^= (hash >>> 16);
        hash *= 0x7feb352d;
        hash ^= (hash >>> 15);
        hash *= 0x846ca68b;
        hash ^= (hash >>> 16);
        return hash;
    }

    private static boolean isPrime(int num) {
        if (num <= 1) return false;
        if (num <= 3) return true;
        if (num % 2 == 0 || num % 3 == 0) return false;
        for (int i = 5; (long) i * i <= num; i += 6) {
            if (num % i == 0 || num % (i + 2) == 0) return false;
        }
        return true;
    }

    private static int findNextPrime(int n) {
        int candidate = Math.max(3, n | 1);
        while (!isPrime(candidate)) candidate += 2;
        return candidate;
    }

    static class Stats {
        long totalProbes;
        long totalOperations;

        long fillNanos;
        long workloadNanos;

        double averageProbes() {
            return totalOperations == 0 ? 0.0 : (double) totalProbes / totalOperations;
        }

        double fillNsPerOp(long fillOps) {
            return fillOps == 0 ? 0.0 : (double) fillNanos / fillOps;
        }

        double workloadNsPerOp(long workloadOps) {
            return workloadOps == 0 ? 0.0 : (double) workloadNanos / workloadOps;
        }
    }

    static class LinearHashTable<K, V> {
        private Object[] keyArray;
        private Object[] valueArray;

        private int numElements;  
        private int slotsUsed;     
        private final double maxLoadFactor;

        public long probesLastOp;

        LinearHashTable(int initialSize, double maxLoad) {
            int capacity = findNextPrime(initialSize);
            this.keyArray = new Object[capacity];
            this.valueArray = new Object[capacity];
            this.maxLoadFactor = maxLoad;
        }

        public int size() { return numElements; }

        public boolean hasKey(K key) { return lookup(key) != null; }

        public V lookup(K key) {
            if (key == null) throw new IllegalArgumentException("Key cannot be null");
            probesLastOp = 0;

            int tableSize = keyArray.length;
            int hash = mixHash(key.hashCode());
            int position = Math.floorMod(hash, tableSize);

            for (int attempt = 0; attempt < tableSize; attempt++) {
                probesLastOp++;
                Object currentKey = keyArray[position];

                if (currentKey == null) return null; 
                if (currentKey != DELETED && currentKey.equals(key)) {
                    @SuppressWarnings("unchecked")
                    V val = (V) valueArray[position];
                    return val;
                }

                position = (position + 1) % tableSize; 
            }
            return null;
        }

        public V insert(K key, V value) {
            if (key == null) throw new IllegalArgumentException("Key cannot be null");

            if ((double) (slotsUsed + 1) / keyArray.length > maxLoadFactor) {
                resize(findNextPrime(keyArray.length * 2));
            }

            probesLastOp = 0;
            int tableSize = keyArray.length;
            int hash = mixHash(key.hashCode());
            int position = Math.floorMod(hash, tableSize);

            int firstDeletedSpot = -1;

            for (int attempt = 0; attempt < tableSize; attempt++) {
                probesLastOp++;
                Object currentKey = keyArray[position];

                if (currentKey == null) {
                    int insertPos = (firstDeletedSpot != -1) ? firstDeletedSpot : position;

                    if (keyArray[insertPos] == null) slotsUsed++;

                    keyArray[insertPos] = key;
                    valueArray[insertPos] = value;
                    numElements++;
                    return null;
                } else if (currentKey == DELETED) {
                    if (firstDeletedSpot == -1) firstDeletedSpot = position;
                } else if (currentKey.equals(key)) {
                    @SuppressWarnings("unchecked")
                    V oldValue = (V) valueArray[position];
                    valueArray[position] = value;
                    return oldValue;
                }

                position = (position + 1) % tableSize;
            }

            throw new IllegalStateException("Hash table is completely full (unexpected).");
        }

        public V delete(K key) {
            if (key == null) throw new IllegalArgumentException("Key cannot be null");
            probesLastOp = 0;

            int tableSize = keyArray.length;
            int hash = mixHash(key.hashCode());
            int position = Math.floorMod(hash, tableSize);

            for (int attempt = 0; attempt < tableSize; attempt++) {
                probesLastOp++;
                Object currentKey = keyArray[position];

                if (currentKey == null) return null; // stop
                if (currentKey != DELETED && currentKey.equals(key)) {
                    @SuppressWarnings("unchecked")
                    V oldValue = (V) valueArray[position];

                    keyArray[position] = DELETED;
                    valueArray[position] = null;
                    numElements--;
 
                    return oldValue;
                }

                position = (position + 1) % tableSize;
            }
            return null;
        }

        private void resize(int newSize) {
            Object[] oldKeys = keyArray;
            Object[] oldValues = valueArray;

            keyArray = new Object[newSize];
            valueArray = new Object[newSize];
            numElements = 0;
            slotsUsed = 0;

            for (int i = 0; i < oldKeys.length; i++) {
                Object k = oldKeys[i];
                if (k != null && k != DELETED) {
                    @SuppressWarnings("unchecked")
                    K actualKey = (K) k;
                    @SuppressWarnings("unchecked")
                    V actualValue = (V) oldValues[i];
                    insert(actualKey, actualValue);
                }
            }
        }
    }

    static class DoubleHashTable<K, V> {
        private Object[] keyArray;
        private Object[] valueArray;

        private int numElements;
        private int slotsUsed;
        private final double maxLoadFactor;

        public long probesLastOp;

        DoubleHashTable(int initialSize, double maxLoad) {
            int capacity = findNextPrime(initialSize);
            this.keyArray = new Object[capacity];
            this.valueArray = new Object[capacity];
            this.maxLoadFactor = maxLoad;
        }

        public int size() { return numElements; }

        public boolean hasKey(K key) { return lookup(key) != null; }

        public V lookup(K key) {
            if (key == null) throw new IllegalArgumentException("Key cannot be null");
            probesLastOp = 0;

            int tableSize = keyArray.length;
            int hash = mixHash(key.hashCode());

            int firstHash = Math.floorMod(hash, tableSize);
            int stepSize = 1 + Math.floorMod(hash, tableSize - 2); 

            int position = firstHash;
            for (int attempt = 0; attempt < tableSize; attempt++) {
                probesLastOp++;
                Object currentKey = keyArray[position];

                if (currentKey == null) return null;
                if (currentKey != DELETED && currentKey.equals(key)) {
                    @SuppressWarnings("unchecked")
                    V val = (V) valueArray[position];
                    return val;
                }

                position = (position + stepSize) % tableSize;
            }
            return null;
        }

        public V insert(K key, V value) {
            if (key == null) throw new IllegalArgumentException("Key cannot be null");

            if ((double) (slotsUsed + 1) / keyArray.length > maxLoadFactor) {
                resize(findNextPrime(keyArray.length * 2));
            }

            probesLastOp = 0;

            int tableSize = keyArray.length;
            int hash = mixHash(key.hashCode());

            int firstHash = Math.floorMod(hash, tableSize);
            int stepSize = 1 + Math.floorMod(hash, tableSize - 2);

            int position = firstHash;
            int firstDeletedSpot = -1;

            for (int attempt = 0; attempt < tableSize; attempt++) {
                probesLastOp++;
                Object currentKey = keyArray[position];

                if (currentKey == null) {
                    int insertPos = (firstDeletedSpot != -1) ? firstDeletedSpot : position;

                    if (keyArray[insertPos] == null) slotsUsed++;

                    keyArray[insertPos] = key;
                    valueArray[insertPos] = value;
                    numElements++;
                    return null;
                } else if (currentKey == DELETED) {
                    if (firstDeletedSpot == -1) firstDeletedSpot = position;
                } else if (currentKey.equals(key)) {
                    @SuppressWarnings("unchecked")
                    V oldValue = (V) valueArray[position];
                    valueArray[position] = value;
                    return oldValue;
                }

                position = (position + stepSize) % tableSize;
            }

            throw new IllegalStateException("Hash table is completely full (unexpected).");
        }

        public V delete(K key) {
            if (key == null) throw new IllegalArgumentException("Key cannot be null");
            probesLastOp = 0;

            int tableSize = keyArray.length;
            int hash = mixHash(key.hashCode());

            int firstHash = Math.floorMod(hash, tableSize);
            int stepSize = 1 + Math.floorMod(hash, tableSize - 2);

            int position = firstHash;
            for (int attempt = 0; attempt < tableSize; attempt++) {
                probesLastOp++;
                Object currentKey = keyArray[position];

                if (currentKey == null) return null;
                if (currentKey != DELETED && currentKey.equals(key)) {
                    @SuppressWarnings("unchecked")
                    V oldValue = (V) valueArray[position];

                    keyArray[position] = DELETED;
                    valueArray[position] = null;
                    numElements--;
                    return oldValue;
                }

                position = (position + stepSize) % tableSize;
            }
            return null;
        }

        private void resize(int newSize) {
            Object[] oldKeys = keyArray;
            Object[] oldValues = valueArray;

            keyArray = new Object[newSize];
            valueArray = new Object[newSize];
            numElements = 0;
            slotsUsed = 0;

            for (int i = 0; i < oldKeys.length; i++) {
                Object k = oldKeys[i];
                if (k != null && k != DELETED) {
                    @SuppressWarnings("unchecked")
                    K actualKey = (K) k;
                    @SuppressWarnings("unchecked")
                    V actualValue = (V) oldValues[i];
                    insert(actualKey, actualValue);
                }
            }
        }
    }

    static int[] generateRandomKeys(int count, Random rng) {
        int[] keys = new int[count];
        for (int i = 0; i < count; i++) keys[i] = rng.nextInt();
        return keys;
    }

    static int[] generateClusteredKeys(int count, Random rng) {
        int baseValue = rng.nextInt() & 0xFFFF0000;
        int[] keys = new int[count];
        for (int i = 0; i < count; i++) {
            keys[i] = baseValue | (i & 0x0000FFFF);
        }
        return keys;
    }

    static class Operation {
        int operationType;
        int key;
        int value;

        Operation(int type, int key, int value) {
            this.operationType = type;
            this.key = key;
            this.value = value;
        }
    }

    static Operation[] generateRealMixedOperations(
            int numOps,
            int[] existingKeys,
            int[] newKeys,
            Random rng
    ) {
        Operation[] operations = new Operation[numOps];

        int newKeyPtr = 0;

        for (int i = 0; i < numOps; i++) {
            int r = rng.nextInt(100);

            if (r < 25) {
                int k;
                if (newKeyPtr < newKeys.length) k = newKeys[newKeyPtr++];
                else k = rng.nextInt(); 
                int v = rng.nextInt();
                operations[i] = new Operation(0, k, v);

            } else if (r < 75) { 
                int pickExisting = rng.nextInt(100);
                int k = (pickExisting < 80)
                        ? existingKeys[rng.nextInt(existingKeys.length)]
                        : newKeys[rng.nextInt(newKeys.length)];
                operations[i] = new Operation(1, k, 0);

            } else { 
                int pickExisting = rng.nextInt(100);
                int k = (pickExisting < 90)
                        ? existingKeys[rng.nextInt(existingKeys.length)]
                        : newKeys[rng.nextInt(newKeys.length)];
                operations[i] = new Operation(2, k, 0);
            }
        }

        return operations;
    }
    // Experiments
    // ---------------------------
    static Stats testLinear(double loadFactor, String workloadType, int numOps, int seed) {
        Random rng = new Random(seed);

        int startCapacity = 200_003;
        LinearHashTable<Integer, Integer> table = new LinearHashTable<>(startCapacity, 0.95);

        int targetSize = (int) (loadFactor * findNextPrime(startCapacity));

        int[] initialKeys = workloadType.equals("clustered")
                ? generateClusteredKeys(targetSize, rng)
                : generateRandomKeys(targetSize, rng);

        Stats stats = new Stats();

        long fillOps = 0;
        long t0 = System.nanoTime();
        for (int i = 0; i < initialKeys.length; i++) {
            table.insert(initialKeys[i], i);
            stats.totalProbes += table.probesLastOp;
            stats.totalOperations++;
            fillOps++;
        }
        long t1 = System.nanoTime();
        stats.fillNanos = (t1 - t0);

        long workloadOps = 0;

        if (workloadType.equals("random") || workloadType.equals("clustered")) {
            int[] opKeys = workloadType.equals("clustered")
                    ? generateClusteredKeys(100_000, rng)
                    : generateRandomKeys(100_000, rng);

            long w0 = System.nanoTime();
            for (int i = 0; i < numOps; i++) {
                int key = opKeys[rng.nextInt(opKeys.length)];
                table.lookup(key);
                stats.totalProbes += table.probesLastOp;
                stats.totalOperations++;
                workloadOps++;
            }
            long w1 = System.nanoTime();
            stats.workloadNanos = (w1 - w0);

        } else {
            int[] existingKeys = initialKeys;
            int[] newKeys = generateRandomKeys(Math.max(1, numOps / 2), rng);

            Operation[] ops = generateRealMixedOperations(numOps, existingKeys, newKeys, rng);

            long w0 = System.nanoTime();
            for (Operation op : ops) {
                if (op.operationType == 0) table.insert(op.key, op.value);
                else if (op.operationType == 1) table.lookup(op.key);
                else table.delete(op.key);

                stats.totalProbes += table.probesLastOp;
                stats.totalOperations++;
                workloadOps++;
            }
            long w1 = System.nanoTime();
            stats.workloadNanos = (w1 - w0);
        }

        return stats;
    }

    static Stats testDouble(double loadFactor, String workloadType, int numOps, int seed) {
        Random rng = new Random(seed);

        int startCapacity = 200_003;
        DoubleHashTable<Integer, Integer> table = new DoubleHashTable<>(startCapacity, 0.95);

        int targetSize = (int) (loadFactor * findNextPrime(startCapacity));

        int[] initialKeys = workloadType.equals("clustered")
                ? generateClusteredKeys(targetSize, rng)
                : generateRandomKeys(targetSize, rng);

        Stats stats = new Stats();

        long fillOps = 0;
        long t0 = System.nanoTime();
        for (int i = 0; i < initialKeys.length; i++) {
            table.insert(initialKeys[i], i);
            stats.totalProbes += table.probesLastOp;
            stats.totalOperations++;
            fillOps++;
        }
        long t1 = System.nanoTime();
        stats.fillNanos = (t1 - t0);

        long workloadOps = 0;

        if (workloadType.equals("random") || workloadType.equals("clustered")) {
            int[] opKeys = workloadType.equals("clustered")
                    ? generateClusteredKeys(100_000, rng)
                    : generateRandomKeys(100_000, rng);

            long w0 = System.nanoTime();
            for (int i = 0; i < numOps; i++) {
                int key = opKeys[rng.nextInt(opKeys.length)];
                table.lookup(key);
                stats.totalProbes += table.probesLastOp;
                stats.totalOperations++;
                workloadOps++;
            }
            long w1 = System.nanoTime();
            stats.workloadNanos = (w1 - w0);

        } else {
            int[] existingKeys = initialKeys;
            int[] newKeys = generateRandomKeys(Math.max(1, numOps / 2), rng);

            Operation[] ops = generateRealMixedOperations(numOps, existingKeys, newKeys, rng);

            long w0 = System.nanoTime();
            for (Operation op : ops) {
                if (op.operationType == 0) table.insert(op.key, op.value);
                else if (op.operationType == 1) table.lookup(op.key);
                else table.delete(op.key);

                stats.totalProbes += table.probesLastOp;
                stats.totalOperations++;
                workloadOps++;
            }
            long w1 = System.nanoTime();
            stats.workloadNanos = (w1 - w0);
        }

        return stats;
    }

    static void runBasicTests() {
        LinearHashTable<String, Integer> linearTable = new LinearHashTable<>(11, 0.9);
        DoubleHashTable<String, Integer> doubleTable = new DoubleHashTable<>(11, 0.9);

        assert linearTable.insert("a", 1) == null;
        assert linearTable.insert("b", 2) == null;
        assert linearTable.lookup("a") == 1;
        assert linearTable.lookup("b") == 2;
        assert linearTable.size() == 2;

        assert doubleTable.insert("a", 1) == null;
        assert doubleTable.insert("b", 2) == null;
        assert doubleTable.lookup("a") == 1;
        assert doubleTable.lookup("b") == 2;
        assert doubleTable.size() == 2;

        assert linearTable.insert("a", 10) == 1;
        assert linearTable.lookup("a") == 10;
        assert doubleTable.insert("a", 10) == 1;
        assert doubleTable.lookup("a") == 10;

        assert linearTable.delete("a") == 10;
        assert linearTable.lookup("a") == null;
        assert linearTable.size() == 1;

        assert doubleTable.delete("a") == 10;
        assert doubleTable.lookup("a") == null;
        assert doubleTable.size() == 1;

        assert !linearTable.hasKey("a");
        assert linearTable.hasKey("b");
        assert !doubleTable.hasKey("a");
        assert doubleTable.hasKey("b");
    }

    public static void main(String[] args) {
        runBasicTests();
        System.out.println("All basic tests passed!");

        double[] loadFactors = {0.5, 0.8, 0.9};
        String[] workloadTypes = {"random", "clustered", "mixed"};
        int operationsPerTest = 300_000;

        System.out.println("\n=== Performance Comparison ===");
        System.out.println("random/clustered: GET-heavy lookups");
        System.out.println("mixed: 25% insert, 50% lookup, 25% delete (mostly existing keys for lookup/delete)");

        for (double loadFactor : loadFactors) {
            for (String workload : workloadTypes) {
                Stats linearStats = testLinear(loadFactor, workload, operationsPerTest, 42);
                Stats doubleStats = testDouble(loadFactor, workload, operationsPerTest, 42);

                int startCapacity = 200_003;
                int targetSize = (int) (loadFactor * findNextPrime(startCapacity));
                long fillOps = targetSize;
                long workloadOps = operationsPerTest;

                System.out.printf("\nLoad factor: %.1f, Workload: %s\n", loadFactor, workload);

                System.out.printf("Linear Probing:\n");
                System.out.printf("  avg probes = %.3f\n", linearStats.averageProbes());
                System.out.printf("  fill ns/op = %.1f (ops=%d)\n", linearStats.fillNsPerOp(fillOps), fillOps);
                System.out.printf("  work ns/op = %.1f (ops=%d)\n", linearStats.workloadNsPerOp(workloadOps), workloadOps);

                System.out.printf("Double Hashing:\n");
                System.out.printf("  avg probes = %.3f\n", doubleStats.averageProbes());
                System.out.printf("  fill ns/op = %.1f (ops=%d)\n", doubleStats.fillNsPerOp(fillOps), fillOps);
                System.out.printf("  work ns/op = %.1f (ops=%d)\n", doubleStats.workloadNsPerOp(workloadOps), workloadOps);
            }
        }

        System.out.println("\nMemory usage: Both implementations use the same amount of memory");
        System.out.println("(two arrays for keys and values; DELETED is a shared sentinel object)");
    }
}
