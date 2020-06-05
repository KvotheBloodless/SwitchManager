package au.com.venilia.switching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import au.com.venilia.switching.service.SwitchService.Circuit;
import au.com.venilia.switching.service.SwitchService.CircuitState;

public class SwitchToggled extends ControllerCommand {

	private final Circuit circuit;

	private final CircuitState circuitState;

	@JsonCreator
	public SwitchToggled(@JsonProperty("circuit") final Circuit circuit,
			@JsonProperty("circuitState") final CircuitState circuitState) {

		this.circuit = circuit;
		this.circuitState = circuitState;
	}

	@JsonProperty
	public Circuit getCircuit() {

		return circuit;
	}

	@JsonProperty
	public CircuitState getCircuitState() {

		return circuitState;
	}
}
