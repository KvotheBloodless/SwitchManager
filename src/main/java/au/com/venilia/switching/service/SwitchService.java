package au.com.venilia.switching.service;

public interface SwitchService {

    public CircuitState getState(final Circuit circuit);

    public void setState(final Circuit circuit, final CircuitState state);

    public void setUnattendedMode(final boolean value);

    public static enum Circuit {

        INTERIOR_LIGHT('a', false),
        ENGINEERING_LIGHT('b', false),
        HYDRAULIC('c', false),
        OUTLET('d', false),
        FAN('e', false),
        VENTILATION('f', true),
        FRESH_WATER_PUMP('g', false),
        WASTE_WATER_PUMP('h', false),
        WASH_DOWN_PUMP('i', false),
        INTERNET('j', false),
        ENTERTAINMENT('k', false),
        REFRIGERATION('l', true),
        ANCHOR_LIGHT('m', true),
        STEAMING_LIGHT('n', false),
        RED_OVER_GREEN_LIGHT('o', false),
        DECK_LIGHT_FORWARD('p', false),
        NAVIGATION_LIGHT('q', false),
        AUTOPILOT('r', false),
        INSTRUMENT('s', false),
        RADIO('t', false),
        DECK_LIGHT_COCKPIT('u', false),
        DECK_LIGHT_AFT('v', false);

        private final char code;

        private final boolean critical;

        private Circuit(final char code, final boolean critical) {

            this.code = code;
            this.critical = critical;
        }

        public char getCode() {

            return code;
        }

        public boolean isCritical() {

            return critical;
        }

        public static Circuit fromCode(final char code) {

            for (final Circuit circuit : values())
                if (circuit.code == code)
                    return circuit;

            throw new EnumConstantNotPresentException(Circuit.class, "" + code);
        }
    }

    public static enum CircuitState {

        OFF(0),
        ON(1);

        private final int code;

        CircuitState(final int code) {

            this.code = code;
        }

        public int getCode() {

            return code;
        }

        public static CircuitState fromCode(final int code) {

            for (final CircuitState state : values())
                if (state.getCode() == code)
                    return state;

            throw new EnumConstantNotPresentException(CircuitState.class, "" + code);
        }
    }
}
