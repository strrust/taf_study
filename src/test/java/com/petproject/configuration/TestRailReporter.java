package com.petproject.configuration;

import lombok.SneakyThrows;
import org.testng.*;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TestRailReporter implements IReporter {

    private Document document;

    //StringXMLBuffer
    //"many-to-one" tcs problem
    //automation_id = classname.name

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        // Iterate over all the suites
        for (ISuite suite : suites) {
            // Get the results for the suite
            Map<String, ISuiteResult> results = suite.getResults();

            // Create a document to store the report
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db;
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
            document = db.newDocument();
            Element testsuites = document.createElement("testsuites");
            document.appendChild(testsuites);

            // Iterate over the results
            for (ISuiteResult result : results.values()) {
                // Get the context for the result
                ITestContext context = result.getTestContext();

                // Get the list of test results
                IResultMap passedTests = context.getPassedTests();
                IResultMap failedTests = context.getFailedTests();
                IResultMap skippedTests = context.getSkippedTests();
                Collection<ITestNGMethod> disabledTests = context.getExcludedMethods();

                // Create a testsuite element for the result
                Element testsuite = document.createElement("testsuite");
                testsuite.setAttribute("name", context.getName());
                testsuite.setAttribute("tests", String.valueOf(context.getAllTestMethods().length));
                testsuite.setAttribute("errors", "0");
                testsuite.setAttribute("skipped", String.valueOf(skippedTests.size() + disabledTests.size()));
                testsuite.setAttribute("failures", String.valueOf(failedTests.size()));
                testsuite.setAttribute("timestamp", context.getStartDate().toString());
                testsuite.setAttribute("time", String.valueOf(context.getEndDate().getTime() - context.getStartDate().getTime()));
                testsuites.appendChild(testsuite);

                // Add the testcase elements for each test result
                for (ITestResult testResult : passedTests.getAllResults()) {
                    addTestCaseElement(testsuite, testResult);
                }
                for (ITestResult testResult : failedTests.getAllResults()) {
                    Element testcase = addTestCaseElement(testsuite, testResult);
                    addFailureElement(testcase, testResult);
                }
                for (ITestResult testResult : skippedTests.getAllResults()) {
                    Element testcase = addTestCaseElement(testsuite, testResult);
                    testcase.appendChild(document.createElement("skipped"));
                }
                for (ITestNGMethod testNGMethod : disabledTests) {
                    Element testcase = addSkippedTestCaseElement(testsuite, testNGMethod);
                    testcase.appendChild(document.createElement("skipped"));
                }
            }

            // Write the report to a file
            try {
                FileOutputStream out = new FileOutputStream(outputDirectory + "/customReport.xml");
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(document), new StreamResult(out));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SneakyThrows
    private Element addTestCaseElement(Element testsuite, ITestResult testResult) {
        // Create a testcase element for the result
        Element testcase = document.createElement("testcase");
        String testRailId = getTestRailId(testResult);
        setTestRailAttributes(testcase, testRailId);
        testcase.setAttribute("realName", testResult.getName());
        testcase.setAttribute("realClassname", testResult.getTestClass().getName());
        testcase.setAttribute("time", String.valueOf(testResult.getEndMillis() - testResult.getStartMillis()));
        testsuite.appendChild(testcase);
        return testcase;
    }

    @SneakyThrows
    private Element addSkippedTestCaseElement(Element testsuite, ITestNGMethod testNGMethod) {
        // Create a testcase element for disabled tests
        Element testcase = document.createElement("testcase");
        String testRailId = getTestRailId(testNGMethod);
        setTestRailAttributes(testcase, testRailId);
        testcase.setAttribute("realName", testNGMethod.getMethodName());
        testcase.setAttribute("realClassname", testNGMethod.getTestClass().getName());
        testsuite.appendChild(testcase);
        return testcase;
    }

    private void addFailureElement(Element testcase, ITestResult testResult) {
        // Create a failure element for failed tests
        Throwable testResultThrowable = testResult.getThrowable();
        Element failure = document.createElement("failure");
        if (testResultThrowable != null) {
            failure.setAttribute("type", testResultThrowable.getClass().getName());
            String message = testResultThrowable.getMessage();
            if (message != null && message.length() > 0) {
                String formattedMessage = message.replace("\r", "").replace("\n", "");
                failure.setAttribute("message", formattedMessage);
            }
            String formattedStackTrace = Utils.shortStackTrace(testResultThrowable, false).replace("\r", "");
            failure.appendChild(document.createCDATASection(formattedStackTrace));
        }
        testcase.appendChild(failure);
    }

    private String getTestRailId(ITestResult testResult) throws NoSuchMethodException {
        String[] testID;
        IClass obj = testResult.getTestClass();
        Class<?> newobj = obj.getRealClass();
        Method testMethod = newobj.getMethod(testResult.getMethod().getMethodName());

        if (testMethod.isAnnotationPresent(TestParameters.class)) {
            TestParameters useAsTestName = testMethod.getAnnotation(TestParameters.class);
            testID = (useAsTestName.testRailId());
            return Arrays.stream(testID).iterator().next();
        } else {
            throw new RuntimeException(String.format("TestRail ID missed for %s", testResult.getMethod().getMethodName()));
        }
    }

    private String getTestRailId(ITestNGMethod testNGMethod) throws NoSuchMethodException {
        String[] testID;
        IClass obj = testNGMethod.getTestClass();
        Class<?> newobj = obj.getRealClass();
        Method testMethod = newobj.getMethod(testNGMethod.getMethodName());

        if (testMethod.isAnnotationPresent(TestParameters.class)) {
            TestParameters useAsTestName = testMethod.getAnnotation(TestParameters.class);
            testID = (useAsTestName.testRailId());
            return Arrays.stream(testID).iterator().next();
        } else {
            throw new RuntimeException(String.format("TestRail ID missed for %s", testNGMethod.getMethodName()));
        }
    }

    private void setTestRailAttributes(Element testcase, String testRailId) {
        testcase.setAttribute("name", testRailId);
        testcase.setAttribute("classname", "TA");
    }
}
