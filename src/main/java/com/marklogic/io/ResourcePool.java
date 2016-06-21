/*
 * Copyright 2003-2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.io;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.xcc.spi.ServerConnection;

public class ResourcePool<K,R> {
    static final Logger logger = Logger.getLogger(ResourcePool.class.getName());
    
    private ConcurrentHashMap<K,Queue<PoolItem<R>>> pools = new ConcurrentHashMap<K,Queue<PoolItem<R>>>();

    public ResourcePool() {
    }

    public boolean isEmpty(K key) {
        Queue<PoolItem<R>> queue = findQueue(key);

        return ((queue == null) || queue.isEmpty());
    }

    public void put(K key, R resource, long expireTimeMillis) {
        addItemToQueue(key, resource, expireTimeMillis);
    }

    public void put(K key, R obj) {
        put(key, obj, -1);
    }

    public R get(K key) {
        return (getItemFromQueue(key));
    }

    public long size(K key) {
        Queue<PoolItem<R>> queue = findQueue(key);

        return ((queue == null) ? 0 : queue.size());
    }

    // --------------------------------------------------------

    // overrideable for unit testing purposes
    protected long getCurrentTime() {
        return (System.currentTimeMillis());
    }

    // --------------------------------------------------------

    private R getItemFromQueue(K key) {
        Queue<PoolItem<R>> queue = findQueue(key);

        if (queue == null) {
            return (null);
        }

        long now = getCurrentTime();
        while (true) {
            PoolItem<R> item = null;

            while (true) {
                item = queue.poll();

                if (item == null)
                    return null;

                if (item.hasExpired(now))
                    item.close();
                else
                    return item.getValue(); 
            }
        }
    }

    private void addItemToQueue(K key, R resource, long expireTimeMillis) {
        PoolItem<R> item = new PoolItem<R>(resource, expireTimeMillis);

        Queue<PoolItem<R>> queue = findOrCreateQueue(key);

        if (!item.hasExpired(getCurrentTime())) {
            queue.add(item);
        } else {
            item.close();
        }
    }

    private Queue<PoolItem<R>> findQueue(Object key) {
        return pools.get(key);
    }

    private Queue<PoolItem<R>> findOrCreateQueue(K key) {
        Queue<PoolItem<R>> queue = findQueue(key);
        if (queue == null) {
            synchronized (pools) {
                queue = new ConcurrentLinkedQueue<PoolItem<R>>(); 
                pools.put(key, queue);
            }
        }
        return queue;
    }
    
    public void closeExpired(long currTime) {
        int count = 0;
        for (Queue<PoolItem<R>> pool : pools.values()) {
            for (PoolItem<R> item : pool) {
                if (item.hasExpired(currTime)) {
                    item.close();
                    pool.remove(item);
                    count++;
                }
            }
        }
        if (count > 0 && logger.isLoggable(Level.FINE)) {
            logger.fine("Closed " + count + " expired items.");
        }
    }

    // --------------------------------------------------------

    protected static class PoolItem<R> {
        private R item;
        private long expireTime;

        public PoolItem(R item, long expireTime) {
            this.item = item;
            this.expireTime = expireTime;
        }

        public R getValue() {
            return (item);
        }

        public boolean hasExpired(long currTime) {
            return ((expireTime != -1) && (currTime >= expireTime));
        }
        
        public void close() {
            try {
                if (item instanceof SocketChannel) {
                    ((SocketChannel)item).close();
                } else if (item instanceof ServerConnection) {
                    ((ServerConnection)item).close();
                }
            } catch (IOException e) {
                // do nothing, channel is being disposed
            }
        }
    }
}
