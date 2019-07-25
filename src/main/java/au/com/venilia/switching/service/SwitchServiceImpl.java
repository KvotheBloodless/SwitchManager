package au.com.venilia.switching.service;

import static au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup.CONTROLLERS;
import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.MASTER;

import java.sql.Connection;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

import au.com.venilia.network.event.DataEvent;
import au.com.venilia.network.service.NetworkCommunicationsService;
import au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup;
import au.com.venilia.switching.dao.SwitchDao;
import au.com.venilia.switching.domain.Switch;
import au.com.venilia.switching.event.RemoteSwitchEvent;

public class SwitchServiceImpl implements SwitchService {

	private final ApplicationEventPublisher eventPublisher;

	private final TaskScheduler scheduler;

	private final NetworkCommunicationsService networkService;

	private final ControllerRoleNegotiationService negotiationService;

	private final SwitchDao switchDao;

	private final long switchStateBroadcastDelaySeconds;

	public SwitchServiceImpl(final ApplicationEventPublisher eventPublisher, final TaskScheduler scheduler,
			final NetworkCommunicationsService networkService,
			final ControllerRoleNegotiationService negotiationService, final Connection connection,
			final long switchStateBroadcastDelaySeconds) {

		this.eventPublisher = eventPublisher;
		this.scheduler = scheduler;
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

		final ParsedCommand parsedCommand = parseCommand(String.valueOf(dataEvent.getData()));
		setState(parsedCommand.getCircuit(), parsedCommand.getCircuitState(), false);
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
				networkService.send(CONTROLLERS, buildCommand(swich).getBytes());
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
