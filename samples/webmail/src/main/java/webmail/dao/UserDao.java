/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.dao;

import webmail.model.User;

import java.util.List;

/** @author Mike Mirzayanov */
public interface UserDao {
    User authenticate(String login, String password);
    boolean register(User user, String password);
    boolean isLoginFree(String login);
    List<String> findLoginsBySubstring(String substring);
    User findByLogin(String login);
}
