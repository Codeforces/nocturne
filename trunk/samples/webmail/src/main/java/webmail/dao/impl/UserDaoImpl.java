/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.dao.impl;

import org.apache.log4j.Logger;
import org.jacuzzi.core.DatabaseException;
import org.jacuzzi.core.GenericDaoImpl;
import org.jacuzzi.core.Row;
import webmail.dao.UserDao;
import webmail.database.ApplicationDataSourceFactory;
import webmail.model.User;

import java.util.List;
import java.util.ArrayList;

/** @author Mike Mirzayanov */
public class UserDaoImpl extends GenericDaoImpl<User, Long> implements UserDao {
    private static final Logger logger = Logger.getLogger(UserDaoImpl.class);

    protected UserDaoImpl() {
        super(ApplicationDataSourceFactory.getInstance());
    }

    @Override
    public User authenticate(String login, String password) {
        return extractSingleUserFromList(findBy("login=? AND password=SHA1(?)", login, password));
    }

    @Override
    public boolean register(User user, String password) {
        beginTransaction();

        try {
            insert(user);

            if (1 != getJacuzzi().execute("UPDATE User SET password=SHA1(?) WHERE id=?", password, user.getId())) {
                throw new DatabaseException("Can't set password for user [id=" + user.getId() + "].");
            }
            commit();
        } catch (Exception e) {
            logger.error("Can't register user.", e);
            rollback();
            return false;
        }

        return true;
    }

    @Override
    public boolean isLoginFree(String login) {
        return getJacuzzi().findLong("SELECT COUNT(*) FROM User WHERE login=?", login) == 0;
    }

    @Override
    public List<String> findLoginsBySubstring(String substring) {
        List<Row> rows = getJacuzzi().findRows("SELECT login FROM User WHERE login LIKE ?",
                "%" + substring + "%");
        List<String> logins = new ArrayList<String>(rows.size());
        for (Row row : rows) {
            logins.add((String) row.get("login"));
        }
        return logins;
    }

    @Override
    public User findByLogin(String login) {
        return extractSingleUserFromList(findBy("login=?", login));
    }

    private User extractSingleUserFromList(List<User> users) {
        if (users.size() == 0) {
            return null;
        }

        if (users.size() > 1) {
            throw new DatabaseException("Expected no more than one user.");
        }

        return users.get(0);
    }
}
