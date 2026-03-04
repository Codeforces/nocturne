package bloggy.dao;

import bloggy.model.Post;
import bloggy.model.User;

import java.util.List;

public interface PostDao {
    Post find(long id);
    List<Post> findAll();
    List<Post> findByUser(User user);
    void insert(Post post);
    void update(Post post);
}
