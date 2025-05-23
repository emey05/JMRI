package jmri.implementation;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import jmri.*;

/**
 * Abstract class providing the basic logic of the Sensor interface.
 *
 * @author Bob Jacobsen Copyright (C) 2001, 2009
 */
public abstract class AbstractSensor extends AbstractNamedBean implements Sensor {


    // ctor takes a system-name string for initialization
    public AbstractSensor(String systemName) {
        super(systemName);
    }

    public AbstractSensor(String systemName, String userName) {
        super(systemName, userName);
    }

    @Override
    @Nonnull
    public String getBeanType() {
        return Bundle.getMessage("BeanNameSensor");
    }

    // implementing classes will typically have a function/listener to get
    // updates from the layout, which will then call
    //  public void firePropertyChange(String propertyName,
    //            Object oldValue,
    //            Object newValue)
    // _once_ if anything has changed state
    @Override
    public int getKnownState() {
        return _knownState;
    }

    protected long sensorDebounceGoingActive = 0L;
    protected long sensorDebounceGoingInActive = 0L;
    protected boolean useDefaultTimerSettings = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSensorDebounceGoingActiveTimer(long time) {
        if (sensorDebounceGoingActive == time) {
            return;
        }
        long oldValue = sensorDebounceGoingActive;
        sensorDebounceGoingActive = time;
        firePropertyChange(PROPERTY_ACTIVE_TIMER, oldValue, sensorDebounceGoingActive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSensorDebounceGoingActiveTimer() {
        return sensorDebounceGoingActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSensorDebounceGoingInActiveTimer(long time) {
        if (sensorDebounceGoingInActive == time) {
            return;
        }
        long oldValue = sensorDebounceGoingInActive;
        sensorDebounceGoingInActive = time;
        firePropertyChange(PROPERTY_INACTIVE_TIMER, oldValue, sensorDebounceGoingInActive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSensorDebounceGoingInActiveTimer() {
        return sensorDebounceGoingInActive;
    }

    @Override
    public void setUseDefaultTimerSettings(boolean boo) {
        if (boo == useDefaultTimerSettings) {
            return;
        }
        useDefaultTimerSettings = boo;
        if (useDefaultTimerSettings) {
            sensorDebounceGoingActive = InstanceManager.sensorManagerInstance().getDefaultSensorDebounceGoingActive();
            sensorDebounceGoingInActive = InstanceManager.sensorManagerInstance().getDefaultSensorDebounceGoingInActive();
        }
        firePropertyChange(PROPERTY_GLOBAL_TIMER, !boo, boo);
    }

    @Override
    public boolean getUseDefaultTimerSettings() {
        return useDefaultTimerSettings;
    }

    protected Thread thr;
    protected Runnable r;

    /**
     * Before going active or inactive or checking that we can go active, we will wait for
     * sensorDebounceGoing(In)Active for things to settle down to help prevent a race condition.
     */
    protected void sensorDebounce() {
        final int lastKnownState = _knownState;
        r = () -> {
            try {
                long sensorDebounceTimer = sensorDebounceGoingInActive;
                if (_rawState == ACTIVE) {
                    sensorDebounceTimer = sensorDebounceGoingActive;
                }
                Thread.sleep(sensorDebounceTimer);
                restartcount = 0;
                _knownState = _rawState;

                javax.swing.SwingUtilities.invokeAndWait(
                        () -> firePropertyChange(PROPERTY_KNOWN_STATE, lastKnownState, _knownState)
                );
            } catch (InterruptedException ex) {
                restartcount++;
            } catch (java.lang.reflect.InvocationTargetException ex) {
                log.error("failed to start debounced Sensor update for \"{}\" due to {}", getDisplayName(), ex.getCause().toString());
            }
        };

        thr = jmri.util.ThreadingUtil.newThread(r , "Debounce thread " + getDisplayName() );
        thr.start();
    }

    int restartcount = 0;

    @Override
    @Nonnull
    @CheckReturnValue
    public String describeState(int state) {
        switch (state) {
            case ACTIVE:
                return Bundle.getMessage("SensorStateActive");
            case INACTIVE:
                return Bundle.getMessage("SensorStateInactive");
            default:
                return super.describeState(state);
        }
    }

    /**
     * Perform setKnownState(int) for implementations that can't actually
     * do it on the layout. Not intended for use by implementations that can.
     */
    @Override
    public void setKnownState(int newState) throws jmri.JmriException {
        setOwnState(newState);
    }

    /**
     * Preprocess a Sensor state change request for specific implementations
     * of {@link #setKnownState(int)}
     *
     * @param newState the Sensor state command value passed
     * @return true if a Sensor.ACTIVE was requested and Sensor is not set to _inverted
     */
    protected boolean stateChangeCheck(int newState) throws IllegalArgumentException {
        // sort out states
        if ((newState & Sensor.ACTIVE) != 0) {
            // first look for the double case, which we can't handle
            if ((newState & Sensor.INACTIVE) != 0) {
                // this is the disaster case!
                throw new IllegalArgumentException("Can't set state for Sensor " + newState);
            } else {
                // send an ACTIVE command (or INACTIVE if inverted)
                return(!getInverted());
            }
        } else {
            // send a INACTIVE command (or ACTIVE if inverted)
            return(getInverted());
        }
    }

    private static final String PROPERTY_RAW_STATE = "RawState";

    /**
     * Set our internal state information, and notify bean listeners.
     *
     * @param s the new state
     */
    public void setOwnState(int s) {
        if (_rawState != s) {
            if (((s == ACTIVE) && (sensorDebounceGoingActive > 0))
                    || ((s == INACTIVE) && (sensorDebounceGoingInActive > 0))) {

                int oldRawState = _rawState;
                _rawState = s;
                if (thr != null) {
                    thr.interrupt();
                }

                if ((restartcount != 0) && (restartcount % 10 == 0)) {
                    log.warn("Sensor \"{}\" state keeps flapping: {}", getDisplayName(), restartcount);
                }
                firePropertyChange(PROPERTY_RAW_STATE, oldRawState, s);
                sensorDebounce();
                return;
            } else {
                // we shall try to stop the thread as one of the state changes
                // might start the thread, while the other may not.
                if (thr != null) {
                    thr.interrupt();
                }
                _rawState = s;
            }
        }
        if (_knownState != s) {
            int oldState = _knownState;
            _knownState = s;
            firePropertyChange(PROPERTY_KNOWN_STATE, oldState, _knownState);
        }
    }

    @Override
    public int getRawState() {
        return _rawState;
    }

    /**
     * Implement a shorter name for setKnownState.
     * <p>
     * This generally shouldn't be used by Java code; use setKnownState instead.
     * The is provided to make Jython script access easier to read.
     */
    @Override
    public void setState(int s) throws jmri.JmriException {
        setKnownState(s);
    }

    /**
     * Implement a shorter name for getKnownState.
     * <p>
     * This generally shouldn't be used by Java code; use getKnownState instead.
     * The is provided to make Jython script access easier to read.
     */
    @Override
    public int getState() {
        return getKnownState();
    }

    /**
     * Control whether the actual sensor input is considered to be inverted,
     * e.g. the normal electrical signal that results in an ACTIVE state now
     * results in an INACTIVE state.
     */
    @Override
    public void setInverted(boolean inverted) {
        boolean oldInverted = _inverted;
        _inverted = inverted;
        if (oldInverted != _inverted) {
            firePropertyChange(PROPERTY_SENSOR_INVERTED, oldInverted, _inverted);
            int state = _knownState;
            if (state == ACTIVE) {
                setOwnState(INACTIVE);
            } else if (state == INACTIVE) {
                setOwnState(ACTIVE);
            }
        }
    }

    /**
     * Get the inverted state. If true, the electrical signal that results in an
     * ACTIVE state now results in an INACTIVE state.
     * <p>
     * Used in polling loops in system-specific code, so made final to allow
     * optimization.
     */
    @Override
    public final boolean getInverted() {
        return _inverted;
    }

    /**
     * By default, all implementations based on this can invert
     */
    @Override
    public boolean canInvert() { return true; }

    protected boolean _inverted = false;

    // internal data members
    protected int _knownState = UNKNOWN;
    protected int _rawState = UNKNOWN;

    Reporter reporter = null;

    /**
     * Some sensor boards also serve the function of being able to report back
     * train identities via such methods as RailCom. The setting and creation of
     * the reporter against the sensor should be done when the sensor is
     * created. This information is not saved.
     *
     * @param er the reporter to set
     */
    @Override
    public void setReporter(Reporter er) {
        reporter = er;
    }

    @Override
    public Reporter getReporter() {
        return reporter;
    }

    /**
     * Set the pull resistance
     * <p>
     * In this default implementation, the input value is ignored.
     *
     * @param r PullResistance value to use.
     */
    @Override
    public void setPullResistance(PullResistance r){
    }

    /**
     * Get the pull resistance.
     *
     * @return the currently set PullResistance value.  In this default
     * implementation, PullResistance.PULL_OFF is always returned.
     */
    @Override
    public PullResistance getPullResistance(){
       return PullResistance.PULL_OFF;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (thr != null) { // try to stop the debounce thread 
            thr.interrupt();
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractSensor.class);

}
