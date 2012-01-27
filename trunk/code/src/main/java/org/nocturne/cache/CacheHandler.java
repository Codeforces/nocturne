package org.nocturne.cache;

import org.nocturne.main.Component;

/**
 * Use implementation of this interface in your pages or frames
 * if you want them to be cached.
 * <p/>
 * Method #intercept will be called after Component.prepareForAction
 * and if it returns non-null string it will be used as parsed result instead
 * of typical life-cycle, i.e. methods initializeAction(), Events.fireBeforeAction(this),
 * ..., Events.fireAfterAction(this), finalizeAction()) will not be called.
 * <p/>
 * But if it returns null, the typical life-cycle will be used and parsed component
 * will be passed as a result to #postprocess().
 *
 * @author Mike Mirzayanov (mirzayanovmr@gmail.com)
 */
public interface CacheHandler {
    String intercept(Component component);

    void postprocess(Component component, String result);
}
