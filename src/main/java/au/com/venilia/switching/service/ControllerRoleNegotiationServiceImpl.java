package au.com.venilia.switching.service;

import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.MASTER;
import static au.com.venilia.switching.service.ControllerRoleNegotiationService.ControllerRole.SLAVE;

import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import com.google.common.collect.Sets;

import au.com.venilia.network.event.PeerDetectionEvent;
import au.com.venilia.network.service.NetworkDiscoveryService;
import au.com.venilia.switching.event.LocalRoleChangeEvent;

public class ControllerRoleNegotiationServiceImpl implements ControllerRoleNegotiationService {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerRoleNegotiationServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;

    private final NetworkDiscoveryService networkDiscoveryService;

    private ControllerRole role = MASTER; // default

    public ControllerRoleNegotiationServiceImpl(final ApplicationEventPublisher eventPublisher,
            final NetworkDiscoveryService networkDiscoveryService) {

        LOG.info("Creating role negotiation service");

        this.eventPublisher = eventPublisher;
        this.networkDiscoveryService = networkDiscoveryService;
    }

    @Override
    public ControllerRole currentRole() {

        return role;
    }

    @EventListener(condition = "#event.moduleGroup == T(au.com.venilia.xbee.service.ModuleDiscoveryService.ModuleGroup).CONTROLLERS")
    public void negotiateRoles(final PeerDetectionEvent event) {

        final int localInstanceId = networkDiscoveryService.getLocalInstanceId();

        // We quite simply look at the known modules (including this one) and take the one with lowest ID as the master
        final TreeSet<Integer> allDevices = Sets.newTreeSet(networkDiscoveryService.getPeerIds(event.getModuleGroup()));
        allDevices.add(localInstanceId);

        // The first device in allDevices list is the master
        if (allDevices.first().intValue() == localInstanceId) {

            if (role != MASTER) {

                role = MASTER;
                eventPublisher.publishEvent(LocalRoleChangeEvent.master(this));
            }
        } else {

            if (role != SLAVE) {

                role = SLAVE;
                eventPublisher.publishEvent(LocalRoleChangeEvent.slave(this));
            }
        }
    }
}
