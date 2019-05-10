package bloggy.web.page;

public class DataPage extends ApplicationPage {
    @Override
    public void initializeAction() {
        super.initializeAction();
        skipTemplate();
    }

    @Override
    public void action() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("WeakerAccess")
    public void printSuccessJson(String... keysAndValues) {
        String[] nextKeysAndValues = new String[keysAndValues.length + 2];
        nextKeysAndValues[0] = "success";
        nextKeysAndValues[1] = "true";
        System.arraycopy(keysAndValues, 0, nextKeysAndValues, 2, keysAndValues.length);
        printJson((Object[]) nextKeysAndValues);
    }
}
