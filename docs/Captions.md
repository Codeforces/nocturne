### Captions

Nocturne makes it very easy to write multilingual applications and conveniently edit various captions, labels, and other text elements.

This mechanism is called Captions, and its essence is that the output of any textual information is wrapped in a caption. For example, in a template, instead of `<td>Login:</td><td><input name="login" class="login"/></td>`, you should write `<td>{{Login}}:</td><td><input name="login" class="login"/></td>`. One of the advantages of this approach is that the templates remain easy to read and are not cluttered with constants like `messages.password-change-confirmation-label`.

In this case, during the template loading stage, Nocturne will process the `{{Login}}` instruction and replace it with the value of the caption named `Login` in the required locale.

Everything that is output and can be language-dependent should go through captions. For example, sometimes this needs to be done in the application code as well. Validator messages should be wrapped, and they are often taken from `ValidationException`:

```java
if (!value.matches("\\w+")) {
    throw new ValidationException($("Field should contain letters, digits and underscore characters"));
}
```

Note the magic method `$()`, which is available to all validators and controllers and can be accessed from any part of the code via `ApplicationContext.getInstance().$()`. It has an overload that allows passing parameters, for example: `$("Field should contain at least {0} characters", minimalLength)`.

In templates, besides the syntax with double curly braces, you can use the caption directive. Examples:

```html
<@caption params=["Mike"]>Hello, {0}</@caption>
<@caption>Login</@caption>
<@caption key="Login"/>
<@caption key="Hello, {0}" params=["Mike"]/>
```

The caption mechanism can also be useful for a monolingual application, as the programmer may not need to think about exact wordings or grammatical correctness, while a specialist can correct or rewrite captions by editing (or translating) them via a web interface.

### interface Captions

All access to captions goes through an implementation of the `org.nocturne.caption.Captions` interface, specific to your application. The simplest implementation is called `org.nocturne.caption.CaptionsImpl` and is the default. It stores captions in properties files depending on the locale (e.g., captions_ru.properties). Furthermore, by analyzing the value of nocturne.debug-captions-dir in debug mode, these files are automatically created and maintained in the corresponding directory. If a value is missing for a locale, then, if it is the default locale, a value matching the caption name is set. Otherwise, the value `nocturne.null` is set, indicating that the value should be taken from the default locale. In production mode, this implementation does not write to properties files but only reads values.

You can write your own implementation of the `org.nocturne.caption.Captions` interface.
