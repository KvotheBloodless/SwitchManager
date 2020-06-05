package au.com.venilia.switching.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = SwitchToggled.class, name = "switch"),
		@Type(value = UnattendedModeToggled.class, name = "unattendedMode") })
public abstract class ControllerCommand {

}
