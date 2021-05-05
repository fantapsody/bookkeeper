/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bookkeeper.meta.store.coordination.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.apache.bookkeeper.common.concurrent.FutureUtils;
import org.apache.bookkeeper.meta.store.api.MetadataStoreException;
import org.apache.bookkeeper.meta.store.api.coordination.CoordinationService;
import org.apache.bookkeeper.meta.store.api.coordination.LeaderElection;
import org.apache.bookkeeper.meta.store.api.coordination.LeaderElectionState;
import org.apache.bookkeeper.meta.store.api.coordination.LockManager;
import org.apache.bookkeeper.meta.store.api.extended.CreateOption;
import org.apache.bookkeeper.meta.store.api.extended.MetadataStoreExtended;

@SuppressWarnings("unchecked")
public class CoordinationServiceImpl implements CoordinationService {

    private final MetadataStoreExtended store;

    private final Map<Class<?>, LockManager<?>> lockManagers = new ConcurrentHashMap<>();
    private final Map<String, LeaderElection<?>> leaderElections = new ConcurrentHashMap<>();

    public CoordinationServiceImpl(MetadataStoreExtended store) {
        this.store = store;
    }

    @Override
    public void close() throws Exception {
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (LeaderElection<?> le : leaderElections.values()) {
                futures.add(le.asyncClose());
            }

            for (LockManager<?> lm : lockManagers.values()) {
                futures.add(lm.asyncClose());
            }

            FutureUtils.collect(futures).join();
        } catch (CompletionException ce) {
            throw MetadataStoreException.unwrap(ce);
        }
    }

    @Override
    public <T> LockManager<T> getLockManager(Class<T> clazz) {
        return (LockManager<T>) lockManagers.computeIfAbsent(clazz, k -> new LockManagerImpl<T>(store, clazz));
    }

    @Override
    public CompletableFuture<Long> getNextCounterValue(String path) {
        String counterBasePath = path + "/-";
        return store
                .put(counterBasePath, new byte[0], Optional.of(-1L),
                        EnumSet.of(CreateOption.Ephemeral, CreateOption.Sequential))
                .thenApply(stat -> {
                    String[] parts = stat.getPath().split("/");
                    String seq = parts[parts.length - 1].replace('-', ' ').trim();
                    return Long.parseLong(seq);
                });
    }

    @Override
    public <T> LeaderElection<T> getLeaderElection(Class<T> clazz, String path,
                                                   Consumer<LeaderElectionState> stateChangesListener) {

        return (LeaderElection<T>) leaderElections.computeIfAbsent(path,
                key -> new LeaderElectionImpl<T>(store, clazz, path, stateChangesListener));
    }
}
