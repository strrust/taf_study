package com.petproject.tests;

import com.petproject.configuration.TestContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

@ContextConfiguration(classes = {TestContextConfiguration.class})
public class SimpleTests extends AbstractTestNGSpringContextTests {

    //passed
    @Test
    public void twoPlusTwoEqualsFour() {
        then(2 + 2).isEqualTo(4);
    }

    //failed with assertion
    @Test
    public void twoPlusTwoEqualsFive() {
        then(2 + 2).isEqualTo(5);
    }

    //disabled
    @Test(enabled = false)
    public void iAmReallySumSomething() {
        //ok, I lied
    }
}
