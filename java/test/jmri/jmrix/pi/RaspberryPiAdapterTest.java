package jmri.jmrix.pi;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioProvider;
import jmri.util.JUnitUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RaspberryPiAdapter
 *
 * @author Paul Bender Copyright (C) 2016
 */
public class RaspberryPiAdapterTest {

   @Test
   public void ConstructorTest(){
       RaspberryPiAdapter a = new RaspberryPiAdapter();
       assertThat(a).isNotNull();
   }

    // The minimal setup for log4J
    @BeforeEach
    public void setUp() {
       JUnitUtil.setUp();
       GpioProvider myprovider = new PiGpioProviderScaffold();
       GpioFactory.setDefaultProvider(myprovider);
       jmri.util.JUnitUtil.resetInstanceManager();
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }

}
