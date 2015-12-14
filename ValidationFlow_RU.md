# Валидация #

В том случае, если для action есть соответствующий validation метод, то сначала вызывается он. Обычно такие методы добавляют в контроллер валидатор и зовут runValidation(). Типичный
validation метод выглядит так:
```
    @Validate("addUser") 
    public boolean validateAddUser() {
        addValidator("name", new LengthValidator(2, 32));
        addValidator("age", new IntegerValidator(1, 200));
        return runValidation();
    }
```

Когда зовется метод runValidation(), то перебираются все parameterNames для которых установлен хотя бы один валидатор, и для каждого из них перебираются валидаторы в порядке добавления. Они запускаются (в них передаются значения соответствующих параметров). Эти параметры в любом случае кладутся во view (т.е. исполняется `put(parameter, getString(parameter))`) и в случае, если валидатор для поля упал, то кладется во view по ключу `"error__" + parameterName` значение, равное сообщению от упавшего валидатора.

Таким образом, в формах во view часто код в форме выглядит так (конечно, он может быть обернут в макрос, чтобы не плодить copy-paste):
```
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

В коде как выше, в случае невалидной формы значение поля будет сохранено, а в случае, если
это поле невалидно, то будет к нему добавлено сообщение об ошибке.

Вот полный код простого контроллера для формы добавления пользователя по заданному имени и возрасту, action ожидается для этого действия равным "add":

```
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