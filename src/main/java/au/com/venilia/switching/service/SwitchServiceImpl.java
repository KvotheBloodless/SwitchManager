package au.com.venilia.switching.service;

import static au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup.CONTROLLERS;
import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.MASTER;

import java.io.IOException;
import java.sql.Connection;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import au.com.venilia.network.event.DataEvent;
import au.com.venilia.network.service.NetworkCommunicationsService;
import au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup;
import au.com.venilia.switching.dao.SwitchDao;
import au.com.venilia.switching.domain.Switch;
import au.com.venilia.switching.event.RemoteSwitchEvent;
import au.com.venilia.switching.model.ControllerCommand;
import au.com.venilia.switching.model.SwitchToggled;
import au.com.venilia.switching.model.UnattendedModeToggled;

public class SwitchServiceImpl implements SwitchService {

	private static final Logger LOG = LoggerFactory.getLogger(SwitchServiceImpl.class);

	private final ApplicationEventPublisher eventPublisher;

	private final TaskScheduler scheduler;

	private final ObjectMapper objectMapper;

	private final NetworkCommunicationsService networkService;

	private final ControllerRoleNegotiationService negotiationService;

	private final SwitchDao switchDao;

	private final long switchStateBroadcastDelaySeconds;

	public SwitchServiceImpl(final ApplicationEventPublisher eventPublisher, final TaskScheduler scheduler,
			final ObjectMapper objectMapper, final NetworkCommunicationsService networkService,
			final ControllerRoleNegotiationService negotiationService, final Connection connection,
			final long switchStateBroadcastDelaySeconds) {

		this.eventPublisher = eventPublisher;
		this.scheduler = scheduler;
		this.objectMapper = objectMapper;
		this.networkService = networkService;
		this.negotiationService = negotiationService;
		this.switchStateBroadcastDelaySeconds = switchStateBroadcastDelaySeconds;

		switchDao = new SwitchDao(connection);

		init();
	}

	private final void init() {

		scheduler.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {

				if (negotiationService.currentRole().equals(MASTER))
					issueUpdateOrder(); // Send a full update of all switch states
			}
		}, Duration.ofSeconds(switchStateBroadcastDelaySeconds));

	}

	@EventListener(condition = "#event.peerGroup == T(au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup).CONTROLLERS")
	private void handleDataEvent(final DataEvent dataEvent) {

		ControllerCommand controllerCommand;
		try {

			controllerCommand = objectMapper.readValue(dataEvent.getData(), ControllerCommand.class);

			if (controllerCommand instanceof SwitchToggled) {

				final SwitchToggled switchToggled = (SwitchToggled) controllerCommand;
				setState(switchToggled.getCircuit(), switchToggled.getCircuitState(), false);
			} else
				LOG.warn("Could not handle ControllerCommand of type {}", controllerCommand.getClass().getSimpleName());
		} catch (final IOException willNotHappen) {

			throw new RuntimeException(willNotHappen);
		}
	}

	// TODO: Move this to the database
	private Map<Circuit, CircuitState> savedState = Maps.newHashMap();

	@Override
	public void setUnattendedMode(final boolean value) {

		if (value) {

			for (final Circuit circuit : Circuit.values()) {

				savedState.put(circuit, getState(circuit));
				if (!circuit.isCritical()) {

					setState(circuit, CircuitState.OFF, true);
					eventPublisher.publishEvent(new RemoteSwitchEvent(this, circuit, CircuitState.OFF));
				}
			}
		} else {

			savedState.entrySet().forEach(e -> {

				setState(e.getKey(), e.getValue(), true);
				eventPublisher.publishEvent(new RemoteSwitchEvent(this, e.getKey(), e.getValue()));
			});

			savedState.clear();
		}

		try {

			networkService.send(CONTROLLERS, objectMapper.writeValueAsBytes(new UnattendedModeToggled(value)));
		} catch (final JsonProcessingException willNotHappen) {

			throw new RuntimeException(willNotHappen);
		}
	}

	@Override
	public CircuitState getState(final Circuit circuit) {

		final Switch swich = switchDao.getByCircuit(circuit);
		return swich == null ? CircuitState.OFF : swich.getState();
	}

	@Override
	public void setState(final Circuit circuit, final CircuitState state) {

		setState(circuit, state, true);
	}

	private void setState(final Circuit circuit, final CircuitState state, final boolean localEvent) {

		boolean updated = false;

		// Switch toggled, save the updated state
		Switch swich = switchDao.getByCircuit(circuit);
		if (swich == null) {

			swich = new Switch(circuit, state);
			updated = true;
		}

		updated |= swich.setState(state);

		switchDao.createOrUpdate(swich);

		if (updated) {

			// Tell the busboxes to update
			if (negotiationService.currentRole().equals(MASTER))
				issueUpdateOrder(swich);

			if (localEvent)
				try {

					networkService.send(CONTROLLERS, objectMapper.writeValueAsBytes(new SwitchToggled(circuit, state)));
				} catch (final JsonProcessingException willNotHappen) {

					throw new RuntimeException(willNotHappen);
				}
			else
				eventPublisher.publishEvent(new RemoteSwitchEvent(this, circuit, state));
		}
	}

	private void issueUpdateOrder(Switch... swich) {

		if (swich.length == 0) {

			final Set<Switch> allSwitchStatus = switchDao.list();
			swich = (Switch[]) allSwitchStatus.toArray(new Switch[allSwitchStatus.size()]);
		}

		// Write the update command to the XBee
		final StringBuffer command = new StringBuffer();

		for (final Switch s : swich)
			command.append(buildCommand(s));

		final byte[] buf = command.toString().getBytes();

		networkService.send(PeerGroup.SWITCHES, buf);
	}

	private static String buildCommand(final Switch swich) {

		return String.format("%c%d;", swich.getCircuit().getCode(), swich.getState().getCode());
	}

	final static Pattern p = Pattern.compile("(\\p{L})(\\p{N})");

	private static ParsedCommand parseCommand(final String command) {

		final Matcher m = p.matcher(command);

		return new ParsedCommand(Circuit.fromCode(m.group(1).charAt(0)),
				CircuitState.fromCode(Integer.parseInt(m.group(2))));
	}

	private static class ParsedCommand {

		private final Circuit circuit;

		private final CircuitState circuitState;

		public ParsedCommand(final Circuit circuit, final CircuitState circuitState) {

			this.circuit = circuit;
			this.circuitState = circuitState;
		}

		public Circuit getCircuit() {

			return circuit;
		}

		public CircuitState getCircuitState() {

			return circuitState;
		}
	}
}
