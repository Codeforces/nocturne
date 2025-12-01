package bloggy.dao.impl;

import bloggy.dao.UserDao;
import bloggy.model.User;
import com.google.inject.Singleton;

@Singleton
public class UserDaoImpl extends ApplicationDaoImpl<User> implements UserDao {
    private static final String PASSWORD_SALT = "5745e88385b83d0dd5e08336579d4bcf";

    @Override
    public User find(long id) {
        return super.find(id);
    }

    @Override
    public User findByName(String name) {
        return findOnlyBy("name=?", name);
    }

    @Override
    public User findByLoginAndPassword(String login, String password) {
        User user = findOnlyBy("login=?", login);
        if (user != null && findCountBy("login=? AND passwordSha=SHA1(CONCAT(id, ?, ?))",
                login, password, PASSWORD_SALT) == 1) {
            return user;
        } else {
            return null;
        }
    }
}
