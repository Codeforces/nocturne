### Proposal for Implementing Caching

Caching Method Calls (Typically for DAO)

Two annotations are used: `@CacheCall` and `@InvalidateCacheCall`. Examples:

```java
public class UserDaoImpl extends ApplicationDaoImpl<User> implements UserDao {
    ...
    @CacheCall(cacheName = "User")
    public User find(long id) {
        ...
    }
    ...
    @InvalidateCacheCall("User")
    public void update(User user) {
        ...
    }
    ...
}
```

@CacheCall expects the following parameters:

1. cacheName - the cache name. The value will be cached in (or retrieved from) the cache with this name.

2. ttl - time to live (in seconds) for the cache, i.e., the maximum lifetime of the cached method execution result.

`@InvalidateCacheCall` expects a value parameter, which can be a string or an array containing the names of caches to be invalidated (completely cleared).

How it works:

1. If a class is loaded via IoC, Google Guice processes it using an interceptor that handles these annotations.

2. When a method annotated with `@CacheCall` is called, a hash value is computed from its arguments array + method name + class name. If the specified cache contains an entry with such a key, the method is not called, and the value from the cache is returned as the result. Otherwise, the method is called, and its return value is stored in the cache under the computed hash key.

3. When a method annotated with `@InvalidateCacheCall` is called, entries in all caches specified as arguments in the annotation are cleared.

If the application is running in debug mode, when processing `@CacheCall`, the method is always called, but its return value is compared with the value stored in the cache. If they do not match, a log entry is written, and a `CacheLogicException` is thrown.

Caching Action Methods of Components (Controllers):

```java
public class UserPage extends Page {
    ...
    @Cacheable
    public void onShowStatus() {
        ...
    }
    ...
}
```
