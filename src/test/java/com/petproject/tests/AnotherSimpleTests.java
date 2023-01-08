package com.petproject.tests;

import com.petproject.configuration.TestContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

@ContextConfiguration(classes = {TestContextConfiguration.class})
public class AnotherSimpleTests {

    @Test
    public void anotherTest() {
        then(23 + 32).isEqualTo(55);
    }
}
