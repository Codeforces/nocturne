/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.pool;

import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic class for any pool.
 *
 * @author Mike Mirzayanov
 */
public abstract class Pool<T> {
    private static final Logger logger = Logger.getLogger(Pool.class);
    private final Queue<T> instances = new LinkedList<>();
    private final AtomicInteger createdCount = new AtomicInteger();

    /**
     * Override it to define the method how pool should get new instance.
     *
     * @return New instance.
     */
    protected abstract T newInstance();

    /**
     * @return Number of new instances created each time the pool is empty.
     */
    protected int getAcquireIncrement() {
        return 5;
    }

    /**
     * Close() method will force finalizeInstance() for each
     * pooled instance.
     *
     * @param t Instance to be finalized.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void finalizeInstance(T t) {
        // No operations.
    }

    /**
     * @return Extracts instance from the pool. Creates new instance if internal pool
     *         storage is empty.
     */
    public T getInstance() {
        synchronized (instances) {
            checkSize();
            ensureElement();
            return instances.remove();
        }
    }

    private void ensureElement() {
        if (instances.isEmpty()) {
            int acquireIncrement = getAcquireIncrement();
            for (int i = 0; i < acquireIncrement; i++) {
                T instance = newInstance();
                instances.add(instance);
                createdCount.incrementAndGet();
            }
        }
    }

    private void checkSize() {
        int acquireIncrement = getAcquireIncrement();
        if (instances.size() > 4 * acquireIncrement) {
            T t = instances.peek();
            if (t != null) {
                logger.warn("Pool queue '" + getClass().getName() + "' [t=" + t.getClass().getName() + "] is too large.");
            }
            while (instances.size() > 2 * acquireIncrement) {
                T instance = instances.remove();
                finalizeInstance(instance);
            }
        }
    }

    /**
     * Instances can be returned into the pool for future reusage.
     *
     * @param instance Instance to be returned into the pool.
     */
    public void release(T instance) {
        synchronized (instances) {
            instances.add(instance);
        }
    }

    /**
     * Finalizes all the instances in the pool and deletes them from the
     * internal storage of the pool.
     */
    public void close() {
        synchronized (instances) {
            while (!instances.isEmpty()) {
                T instance = instances.remove();
                finalizeInstance(instance);
            }
        }
    }

    /**
     * @return Total count of the created instances by this pool.
     */
    public int getCreatedCount() {
        return createdCount.get();
    }
}
