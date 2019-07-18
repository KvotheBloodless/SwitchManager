package au.com.venilia.switching.service;

import static au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup.CONTROLLERS;
import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.MASTER;

import java.sql.Connection;
import java.time.Duration;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;

import au.com.venilia.network.event.DataEvent;
import au.com.venilia.network.service.NetworkCommunicationsService;
import au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup;
import au.com.venilia.switching.dao.SwitchDao;
import au.com.venilia.switching.domain.Switch;

public class SwitchServiceImpl implements SwitchService {

    private final TaskScheduler scheduler;

    private final NetworkCommunicationsService networkService;

    private final ControllerRoleNegotiationService negotiationService;

    private final SwitchDao switchDao;

    private final long switchStateBroadcastDelaySeconds;

    public SwitchServiceImpl(final TaskScheduler scheduler,
            final NetworkCommunicationsService networkService,
            final ControllerRoleNegotiationService negotiationService,
            final Connection connection,
            final long switchStateBroadcastDelaySeconds) {

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

                if (negotiationService.currentRole().equals(MASTER)) {

                    // TODO Send out the full set of switch states
                }
            }
        }, Duration.ofSeconds(switchStateBroadcastDelaySeconds));

    }

    @EventListener(condition = "#event.peerGroup == T(au.com.venilia.network.service.NetworkCommunicationsService.PeerGroup).CONTROLLERS")
    private void handleDataEvent(final DataEvent dataEvent) {

        // TODO: parse and call setState(circuit, state, false)
        // TODO: Dispatch an event to update the UI
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

    private void setState(final Circuit circuit, final CircuitState state, final boolean tellPeers) {

        // Switch toggled, save the updated state
        final Switch swich = switchDao.getByCircuit(circuit);
        if (swich != null) {

            if (swich.setState(state)) {

                switchDao.createOrUpdate(swich);

                // Tell the busboxes to update
                if (negotiationService.currentRole().equals(MASTER))
                    issueUpdateOrder(swich);
                else if (tellPeers)
                    networkService.send(CONTROLLERS, "".getBytes()); // TODO: Tell other controllers
            }
        } else {

            final Switch newSwich = new Switch(circuit, state);

            switchDao.createOrUpdate(newSwich);
            if (negotiationService.currentRole().equals(MASTER))
                issueUpdateOrder(newSwich);
            else if (tellPeers)
                networkService.send(CONTROLLERS, "".getBytes()); // TODO: Tell other controllers
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
            command.append(String.format("%c%d;", s.getCircuit().getCode(), s.getState().getCode()));

        final byte[] buf = command.toString().getBytes();

        networkService.send(PeerGroup.SWITCHES, buf);
    }
}
