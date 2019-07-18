package au.com.venilia.switching.service;

public interface SwitchService {

    public CircuitState getState(final Circuit circuit);

    public void setState(final Circuit circuit, final CircuitState state);

    public static enum Circuit {

        INTERIOR_LIGHT('a'),
        ENGINEERING_LIGHT('b'),
        HYDRAULIC('c'),
        OUTLET('d'),
        FAN('e'),
        VENTILATION('f'),
        FRESH_WATER_PUMP('g'),
        WASTE_WATER_PUMP('h'),
        WASH_DOWN_PUMP('i'),
        INTERNET('j'),
        ENTERTAINMENT('k'),
        REFRIGERATION('l'),
        ANCHOR_LIGHT('m'),
        STEAMING_LIGHT('n'),
        RED_OVER_GREEN_LIGHT('o'),
        DECK_LIGHT('p'),
        NAVIGATION_LIGHT('q'),
        AUTOPILOT('r'),
        INSTRUMENT('s'),
        RADIO('t');

        private final char code;

        private Circuit(final char code) {

            this.code = code;
        }

        public char getCode() {

            return code;
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
