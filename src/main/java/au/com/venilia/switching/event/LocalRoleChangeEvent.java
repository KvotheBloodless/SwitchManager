package au.com.venilia.switching.event;

import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.MASTER;
import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.SLAVE;

import org.springframework.context.ApplicationEvent;

import au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole;

public class LocalRoleChangeEvent extends ApplicationEvent {

    private final ControllerRole role;

    public LocalRoleChangeEvent(final Object source, final ControllerRole role) {

        super(source);

        this.role = role;
    }

    public ControllerRole getRole() {

        return role;
    }

    public static LocalRoleChangeEvent master(final Object source) {

        return new LocalRoleChangeEvent(source, MASTER);
    }

    public static LocalRoleChangeEvent slave(final Object source) {

        return new LocalRoleChangeEvent(source, SLAVE);
    }
}
