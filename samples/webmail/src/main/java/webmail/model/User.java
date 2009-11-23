/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.model;

import org.jacuzzi.mapping.Id;

/** @author Mike Mirzayanov */
public class User {
    @Id
    private long id;

    private String name;

    private String login;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
