/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import org.nocturne.exception.IncorrectLogicException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Use it to listen events and fire them. Any object can be event. Listeners
 * are subscribed to class. When executed fire(event) => all listeners for class
 * event.getClass() will be notified. Also all listeners
 * for event.getClass().getSuperclass() (and so on) will be notified.
 * <p/>
 * Use pair of methods beforeAction() and afterAction() to listen components.
 * Any component will notify all listeners registered with beforeAction()
 * before process action and will notify all listeners registered with afterAction()
 * after process action.
 * <p/>
 * See also <a href="http://code.google.com/p/nocturne/wiki/RequestLifeCycle_RU">http://code.google.com/p/nocturne/wiki/RequestLifeCycle_RU</a>
 * or <a href="http://code.google.com/p/nocturne/wiki/RequestLifeCycle_EN">http://code.google.com/p/nocturne/wiki/RequestLifeCycle_EN</a>
 *
 * @author Mike Mirzayanov
 */
public class Events {
    /**
     * Each class has no more than MAX_LISTENER_COUNT listeners.
     * If you are try to add more, an exception will be thrown.
     * Usually it means that you are trying to add listeners on each request,
     * but you shouldn't do it
     */
    private static final int MAX_LISTENER_COUNT = 20;

    private static final Scope COMMON_SCOPE = new Scope();
    private static final Scope BEFORE_ACTION_SCOPE = new Scope();
    private static final Scope AFTER_ACTION_SCOPE = new Scope();

    /**
     * Add listener to events of class "eventClass".
     *
     * @param eventClass Class to be listen. If event has "eventClass" as its
     *                   superclass listeners will be notified too.
     * @param listener   Listener instance.
     */
    public static <T> void listen(Class<T> eventClass, Listener<T> listener) {
        COMMON_SCOPE.listen(eventClass, listener);
    }

    /**
     * @param event Throwing event. All listeners registered for class event.getClass()
     *              or its superclass will be notified.
     */
    public static <T> void fire(T event) {
        COMMON_SCOPE.fire(event);
    }

    /**
     * @param componentClass Component class to be listen.
     * @param listener       Listener which will be notified before any action
     *                       for componentClass will be processed.
     */
    public static <T extends Component> void beforeAction(Class<T> componentClass, Listener<T> listener) {
        BEFORE_ACTION_SCOPE.listen(componentClass, listener);
    }

    static void fireBeforeAction(Component component) {
        BEFORE_ACTION_SCOPE.fire(component);
    }

    /**
     * @param componentClass Component class to be listen.
     * @param listener       Listener which will be notified after any action
     *                       for componentClass will be processed.
     */
    public static <T extends Component> void afterAction(Class<T> componentClass, Listener<T> listener) {
        AFTER_ACTION_SCOPE.listen(componentClass, listener);
    }

    static void fireAfterAction(Component component) {
        AFTER_ACTION_SCOPE.fire(component);
    }

    private static class Scope {
        /** Stores listeners for each . */
        private Map<Class<?>, Set<Listener>> listenersByEvent =
                new LinkedHashMap<Class<?>, Set<Listener>>();

        public <T> void listen(Class<T> eventClass, Listener<T> listener) {
            if (!listenersByEvent.containsKey(eventClass)) {
                listenersByEvent.put(eventClass, new LinkedHashSet<Listener>());
            }

            listenersByEvent.get(eventClass).add(listener);

            if (listenersByEvent.get(eventClass).size() > MAX_LISTENER_COUNT) {
                throw new IncorrectLogicException("Too many listeners for " + eventClass.getName() + " event type. " +
                        "Are you sure you code is correct?");
            }
        }

        @SuppressWarnings({"unchecked"})
        public <T> void fire(T event) {
            Class<? super T> clazz = (Class<? super T>) event.getClass();
            while (clazz != null) {
                fireExactMatchedListeners(event, clazz);
                clazz = clazz.getSuperclass();
            }
        }

        @SuppressWarnings({"unchecked"})
        private <T> void fireExactMatchedListeners(T event, Class<? super T> clazz) {
            if (listenersByEvent.containsKey(clazz)) {
                Set<Listener> listeners = listenersByEvent.get(clazz);
                for (Listener<?> listener : listeners) {
                    Listener<? super T> t = (Listener<? super T>) listener;
                    t.onEvent(event);
                }
            }
        }

    }
}
