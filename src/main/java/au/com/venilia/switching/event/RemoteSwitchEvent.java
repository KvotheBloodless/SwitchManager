package au.com.venilia.switching.event;

import org.springframework.context.ApplicationEvent;

import au.com.venilia.switching.service.SwitchService.Circuit;
import au.com.venilia.switching.service.SwitchService.CircuitState;

public class RemoteSwitchEvent extends ApplicationEvent {

    private final Circuit circuit;
    private final CircuitState state;

    public RemoteSwitchEvent(final Object source, final Circuit circuit, final CircuitState state) {

        super(source);

        this.circuit = circuit;
        this.state = state;
    }

    public Circuit getCircuit() {

        return circuit;
    }

    public CircuitState getState() {

        return state;
    }
}
