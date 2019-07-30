package au.com.venilia.switching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UnattendedModeToggled extends ControllerCommand {

	private final boolean state;

	@JsonCreator
	public UnattendedModeToggled(@JsonProperty("state") final boolean state) {

		this.state = state;
	}

	@JsonProperty
	private boolean getState() {

		return state;
	}
}
