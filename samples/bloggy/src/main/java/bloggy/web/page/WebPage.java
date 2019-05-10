package bloggy.web.page;

public abstract class WebPage extends ApplicationPage {
    @Override
    public void finalizeAction() {
        super.finalizeAction();

        String message = getMessage();
        if (message != null) {
            put("message", message);
        }
    }

    private String getMessage() {
        if (hasSession(MESSAGE_SESSION_KEY)) {
            String message = getSession(MESSAGE_SESSION_KEY, String.class);
            removeSession(MESSAGE_SESSION_KEY);
            if (message != null && !message.isEmpty()) {
                return message;
            }
        }
        return null;
    }
}
