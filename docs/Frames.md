### Frames

Often, some complex pages need to be decomposed because different pages contain identical elements (for example, a news panel). Nocturne supports extracting such components into frames. Each frame has its own controller (a class inherited from org.nocturne.main.Frame) and can have its own template. The frame's lifecycle mirrors that of the page and begins when code like `parse("parsedFrame", someFrame)` (or simply `parse(someFrame)`) is called for a given frame named someFrame. In short:

1. The frame is initialized if necessary (exactly once per instance).

2. initializeAction()

3. An event is fired, triggering subscribers of Events.beforeAction(SomeFrame.class, ...).

4. The validation method runs if needed. It uses the current page's action.

5. The action method runs (if validation passed).

6. The invalid method runs (if validation failed).

7. An event is fired, triggering subscribers of Events.afterAction(SomeFrame.class, ...).

8. finalizeAction()

The component that called `parse("parsedFrame", someFrame)` now has the frame's rendering result stored in its internal map under the key "parsedFrame". Now, if the template of this component contains `<@frame name="parsedFrame"/>`, the frame's rendering result will be inserted in that place. Thus, pages and other frames can include frames.

In components that contain frames, it is best to instantiate them via IoC using the @Inject annotation.

### Example of Working with a Frame

```java
@Link(";home")
public class IndexPage extends Page {
    @Inject
    private LoginFormFrame loginForm;

    public void action() {
        parse("loginForm", loginForm);
    }
}
```

And the IndexPage.ftl template:

```ftl
<#import "macros/common.ftl" as common>

<@common.page>
    <@frame name="loginForm"/>
    ...
</@common.page>
```
