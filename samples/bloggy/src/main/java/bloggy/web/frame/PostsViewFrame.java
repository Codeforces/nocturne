package bloggy.web.frame;

import com.google.inject.Inject;
import bloggy.dao.PostDao;
import bloggy.model.Post;
import bloggy.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class PostsViewFrame extends ApplicationFrame {
    @Inject
    private PostDao postDao;

    private User postsUser;

    public void setPostsUser(User postsUser) {
        this.postsUser = postsUser;
    }

    @Override
    public void action() {
        putPosts(postsUser == null ? postDao.findAll() : postDao.findByUser(postsUser));
    }

    private void putPosts(List<Post> posts) {
        for (Post post : posts) {
            PostViewFrame postViewFrame = getInstance(PostViewFrame.class);
            postViewFrame.setPost(post);
            postViewFrame.setShortMode(true);
            parse("post" + post.getId(), postViewFrame);
        }
        put("postIds", posts.stream().map(Post::getId).collect(Collectors.toList()));
    }
}
