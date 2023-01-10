package com.petproject.tests;

import com.petproject.configuration.TestContextConfiguration;
import com.petproject.configuration.TestParameters;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

@ContextConfiguration(classes = {TestContextConfiguration.class})
public class AnotherSimpleTests {

    @Test
    @TestParameters(testRailId = "C35")
    public void anotherTest() {
        then(23 + 32).isEqualTo(55);
    }
}
