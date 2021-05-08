# `Minimalism` Project Overview

## Works done

### Broker
- Make the pulsar setup command to adapt the new metadataStore
- Implement a `MetadataStore` with the embedded Raft KV implementation
- Make all the Zookeeper operations adapt with the new `MetadataStore` implementation
- Make `ManagedLedgerFactory` and `BookKeeperClientFactory` started without Zookeeper

### bookkeeper
- Embed a Raft KV implementation (JRaft Rhea KV) into BookKeeper and make it start with Bookies
- Migrate the `MetadataStore` interface into bookkeeper
- Implement an adapter the make the metadata driver in bookkeeper leverage the underlying `MetadataStore` instance
- Implement a `MetadataStore` implementation for our embedded Raft KV implementation

## TODOs

### Raft MetadataStore implementation
- Watch and ephemeral nodes are not fully supported yet
- Distribute ID generator

### Broker
- Better abstraction of the pulsar metadata operations

### Bookkeeper
- Better integration between bookie and raft KV
- Raft group implementation
