package au.com.venilia.switching.domain;

import com.google.common.base.MoreObjects;

import au.com.venilia.switching.service.SwitchService.Circuit;
import au.com.venilia.switching.service.SwitchService.CircuitState;

public class Switch {

    private Integer id;

    private Circuit circuit;

    private CircuitState state;

    public Switch(final Integer id, final Circuit circuit, final CircuitState state) {

        this(circuit, state);
        this.id = id;
    }

    public Switch(final Circuit circuit, final CircuitState state) {

        this.circuit = circuit;
        this.state = state;
    }

    public Integer getId() {

        return id;
    }

    public void setId(final Integer id) {

        this.id = id;
    }

    public Circuit getCircuit() {

        return circuit;
    }

    public void setCircuit(final Circuit circuit) {

        this.circuit = circuit;
    }

    public CircuitState getState() {

        return state;
    }

    public boolean setState(final CircuitState state) {

        if (this.state != state) {

            this.state = state;
            return true;
        }

        return false;
    }

    @Override
    public String toString() {

        return MoreObjects.toStringHelper(this).add("circuit", circuit).add("state", state).toString();
    }
}
