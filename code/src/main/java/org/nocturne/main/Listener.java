/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

/**
 * Event listener.
 *
 * @author Mike Mirzayanov
 */
public interface Listener<T> {
    /**
     * This method will be invoked in case of incovation
     * Events.fire(event) and if the listener was
     * subscribed to event.getClass() or its superclass.
     *
     * @param event Event instance.
     */
    void onEvent(T event);
}
