package jmri.jmrit.operations.locations.schedules;

import java.awt.GraphicsEnvironment;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import jmri.jmrit.operations.OperationsTestCase;
import jmri.util.JUnitOperationsUtil;
import jmri.util.JUnitUtil;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
public class SchedulesByLoadFrameTest extends OperationsTestCase {

    @Test
    public void testCTor() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        jmri.util.JUnitOperationsUtil.initOperationsData();
        SchedulesByLoadFrame t = new SchedulesByLoadFrame();
        Assert.assertNotNull("exists",t);
        JUnitUtil.dispose(t);

    }

    // private final static Logger log = LoggerFactory.getLogger(SchedulesByLoadFrameTest.class);

}
