package jmri.jmrix;

import jmri.util.JUnitUtil;
import jmri.InstanceManager;
import jmri.NamedBean;

import java.util.Comparator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
public class SystemConnectionMemoTest {

    private SystemConnectionMemo _memo = null;

    @Test
    public void testCTor() {
        Assert.assertNotNull("exists", _memo);
    }

    @Test
    public void testGetConsistManagerNull() {
        Assert.assertNull("null consist manager", _memo.get(jmri.ConsistManager.class));
    }

    @Test
    public void testProvidesConsistManagerNull() {
        Assert.assertFalse("null consist manager", _memo.provides(jmri.ConsistManager.class));
    }

    @Test
    public void testGetConsistManagerWithCS() {
        SystemConnectionMemo m = new SystemConnectionMemo("T", "Test") {
            @Override
            protected java.util.ResourceBundle getActionModelResourceBundle() {
                return null;
            }

            @Override
            public <B extends NamedBean> Comparator<B> getNamedBeanComparator(Class<B> type) {
                return null;
            }

            @Override
            public boolean provides(Class<?> c) {
                if (c.equals(jmri.CommandStation.class)) {
                    return true;
                }
                return super.provides(c);
            }

            @Override
            @SuppressWarnings("unchecked") // dynamic check
            public <T> T get(Class<?> T) {
                if (T.equals(jmri.CommandStation.class)) {
                    return (T) InstanceManager.getDefault(T);
                }
                return super.get(T);
            }

        };
        Assert.assertNotNull("consist manager", m.get(jmri.ConsistManager.class));
    }

    @Test
    public void testProvidesConsistManagerWithCS() {
        SystemConnectionMemo t = new SystemConnectionMemo("T", "Test") {
            @Override
            protected java.util.ResourceBundle getActionModelResourceBundle() {
                return null;
            }

            @Override
            public <B extends NamedBean> Comparator<B> getNamedBeanComparator(Class<B> type) {
                return null;
            }

            @Override
            public boolean provides(Class<?> c) {
                if (c.equals(jmri.CommandStation.class)) {
                    return true;
                }
                return super.provides(c);
            }

            @Override
            @SuppressWarnings("unchecked") // dynamic check
            public <T> T get(Class<?> T) {
                if (T.equals(jmri.CommandStation.class)) {
                    return (T) InstanceManager.getDefault(T);
                }
                return super.get(T);
            }
        };
        Assert.assertTrue("null consist manager", t.provides(jmri.ConsistManager.class));
    }

    @Test
    public void testGetConsistManagerWithAPM() {
        SystemConnectionMemo t = new SystemConnectionMemo("T", "Test") {
            @Override
            protected java.util.ResourceBundle getActionModelResourceBundle() {
                return null;
            }

            @Override
            public <B extends NamedBean> Comparator<B> getNamedBeanComparator(Class<B> type) {
                return null;
            }

            @Override
            public boolean provides(Class<?> c) {
                if (c.equals(jmri.AddressedProgrammerManager.class)) {
                    return true;
                }
                return super.provides(c);
            }

            @Override
            @SuppressWarnings("unchecked") // dynamic check
            public <T> T get(Class<?> T) {
                if (T.equals(jmri.AddressedProgrammerManager.class)) {
                    return (T) InstanceManager.getDefault(T);
                }
                return super.get(T);
            }

        };
        Assert.assertNotNull("consist manager", t.get(jmri.ConsistManager.class));
    }

    @Test
    public void testProvidesConsistManagerWithAPM() {
        SystemConnectionMemo t = new SystemConnectionMemo("T", "Test") {
            @Override
            protected java.util.ResourceBundle getActionModelResourceBundle() {
                return null;
            }

            @Override
            public <B extends NamedBean> Comparator<B> getNamedBeanComparator(Class<B> type) {
                return null;
            }

            @Override
            public boolean provides(Class<?> c) {
                if (c.equals(jmri.AddressedProgrammerManager.class)) {
                    return true;
                }
                return super.provides(c);
            }

            @Override
            @SuppressWarnings("unchecked") // dynamic check
            public <T> T get(Class<?> T) {
                if (T.equals(jmri.AddressedProgrammerManager.class)) {
                    return (T) InstanceManager.getDefault(T);
                }
                return super.get(T);
            }
        };
        Assert.assertTrue("null consist manager", t.provides(jmri.ConsistManager.class));
    }

    @Before
    public void setUp() {
        JUnitUtil.setUp();
        JUnitUtil.initDebugCommandStation();
        JUnitUtil.initDebugProgrammerManager();
        _memo = new SystemConnectionMemo("T", "Test") {
            @Override
            protected java.util.ResourceBundle getActionModelResourceBundle() {
                return null;
            }

            @Override
            public <B extends NamedBean> Comparator<B> getNamedBeanComparator(Class<B> type) {
                return null;
            }
        };
    }

    @After
    public void tearDown() {
        JUnitUtil.tearDown();
    }

    // private final static Logger log = LoggerFactory.getLogger(SystemConnectionMemoTest.class);
}
