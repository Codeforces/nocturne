/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

/** @author Mike Mirzayanov */
public abstract class DataPage extends ApplicationPage {
    @Override
    public void initializeAction() {
        super.initializeAction();
        skipTemplate();
    }
}
