package au.com.venilia.switching.exception;

public class SwitchNotFoundException extends Exception {

    public SwitchNotFoundException(final int id) {

        super(String.format("Could not load switch with id %d", id, id));
    }
}
