package com.petproject.tests;

import com.petproject.configuration.TestContextConfiguration;
import com.petproject.configuration.TestParameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

@ContextConfiguration(classes = {TestContextConfiguration.class})
public class SimpleTests extends AbstractTestNGSpringContextTests {

    //passed
    @Test
    @TestParameters(testRailId = "C34")
    public void twoPlusTwoEqualsFour() {
        then(2 + 2).isEqualTo(4);
    }

    //failed with assertion
    @Test
    @TestParameters(testRailId = "C32")
    public void twoPlusTwoEqualsFive() {
        then(2 + 2).isEqualTo(5);
    }

    //disabled
    @Test(enabled = false)
    @TestParameters(testRailId = "C31")
    public void iAmReallySumSomething() {
        //ok, I lied
    }
}
