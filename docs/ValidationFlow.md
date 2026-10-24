### Validation

If there is a corresponding validation method for an action, it is called first. Typically, such methods add a validator to the controller and call runValidation(). A typical validation method looks like this:

```java
@Validate("addUser")
public boolean validateAddUser() {
    addValidator("name", new LengthValidator(2, 32));
    addValidator("age", new IntegerValidator(1, 200));
    return runValidation();
}
```

When the runValidation() method is called, all parameterNames for which at least one validator has been set are iterated, and for each of them, the validators are processed in the order they were added. They are executed (with the values of the corresponding parameters passed to them). These parameters are always placed into the view (i.e., `put(parameter, getString(parameter))` is executed), and if a validator for a field fails, a value equal to the message from the failed validator is placed into the view under the key `"error__" + parameterName`.

Thus, in forms within the view, the code often looks like this (of course, it can be wrapped in a macro to avoid copy-paste):

```ftl
<tr>
    <td>
        Login:
    </td>
    <td>
        <input name="login" value="${login!}"/>
    </td>
</tr>
<#if error__login??>
<tr>
    <td>&nbsp;</td>
    <td><span class="field-error">${error__login!}</span></td>
</tr>
</#if>
```

In the code above, if the form is invalid, the field value will be preserved, and if the field is invalid, an error message will be added to it.

Here is the complete code for a simple controller for a form to add a user with a given name and age. The action expected for this operation is "add":

```java
@Link("user/{action}")
public class UserChangePage extends Page {
    @Parameter(stripMode = Parameter.StripMode.SAFE)
    private String name;

    @Parameter
    private int age;

    @Inject
    private UserDao userDao;

    @Validate("add")
    public boolean validateAddUser() {
        addValidator("name", new LengthValidator(2, 32));
        addValidator("age", new IntegerValidator(1, 200));
        return runValidation();
    }

    @Action("add")
    public void onAddUser() {
        userDao.add(new User(name, age));
    }
}
```
