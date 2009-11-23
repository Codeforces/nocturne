/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.pool;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Generic class for any pool.
 *
 * @author Mike Mirzayanov
 */
public abstract class Pool<T> {
    private final Queue<T> instances = new LinkedList<T>();

    /**
     * Override it to define the method how pool should get new instance.
     *
     * @return New instance.
     */
    protected abstract T newInstance();

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
            if (instances.isEmpty()) {
                return newInstance();
            } else {
                return instances.remove();
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
}
