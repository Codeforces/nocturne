package bloggy.dao.impl;

import bloggy.dao.PostDao;
import bloggy.model.Post;
import bloggy.model.User;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class PostDaoImpl extends ApplicationDaoImpl<Post> implements PostDao {
    @Override
    public Post find(long id) {
        return super.find(id);
    }

    @Override
    public List<Post> findAll() {
        return findBy("NOT deleted ORDER BY updateTime DESC, id DESC");
    }

    @Override
    public List<Post> findByUser(User user) {
        return findBy("userId=? AND NOT deleted ORDER BY updateTime DESC, id DESC", user.getId());
    }
}
