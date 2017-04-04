package org.nocturne.module;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 04.04.2017
 */
public interface StoppableConfiguration extends Configuration {
    /**
     * Sends a stop signal to the module.
     * <p>
     * This method should return immediately, even if shutdown logic is not yet completed.
     */
    void stop();
}
