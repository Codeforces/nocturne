package org.nocturne.pool;

import java.util.LinkedList;
import java.util.Queue;

/** @author Mike Mirzayanov */
public abstract class Pool<T> {
    private final Queue<T> instances = new LinkedList<T>();

    protected abstract T newInstance();

    protected void finalizeInstance(T t) {
        // No operations.
    }

    public T getInstance() {
        synchronized (instances) {
            if (instances.isEmpty()) {
                return newInstance();
            } else {
                return instances.remove();
            }
        }
    }

    public void release(T instance) {
        synchronized (instances) {
            instances.add(instance);
        }
    }

    public void close() {
        synchronized (instances) {
            while (!instances.isEmpty()) {
                T instance = instances.remove();
                finalizeInstance(instance);
            }
        }
    }
}
