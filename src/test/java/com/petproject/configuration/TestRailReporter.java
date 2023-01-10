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

/**
 * The class represents an approach to solving a problem when several autotests corresponds to one manual test case
 * (one-to-many problem) in TestRail with TR CLI usage by generating special report for TR CLI:
 * 1) Automation ID field now depends on TestRail manual test case id
 * 2) Manual test case id must be specified with annotation @TestParameters(testRailId = "C35")
 * 3) It is possible to specify one TestRail manual case ID for few autotests.
 *
 * Example:
 * @Test
 * @TestParameters(testRailId = "C35")
 * void firstTest() {
 * //some code
 * }
 * @Test
 * @TestParameters(testRailId = "C35")
 * void secondTest() {
 * //some code
 * }
 *
 * Due to the feature of TestRail report parsing and TestRailReporter features (failed and skipped tests coming first)
 * if one linked autotest will fail manual test case linked by ID will be marked as "Failed" after report is loaded.
 */
public class TestRailReporter implements IReporter {
    private Document reportDocument;

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        for (ISuite suite : suites) {
            Map<String, ISuiteResult> results = suite.getResults();

            DocumentBuilder reportDocumentBuilder;
            try {
                reportDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
            reportDocument = reportDocumentBuilder.newDocument();
            Element testSuites = reportDocument.createElement("testsuites");
            reportDocument.appendChild(testSuites);

            for (ISuiteResult result : results.values()) {
                ITestContext context = result.getTestContext();
                IResultMap passedTests = context.getPassedTests();
                IResultMap failedTests = context.getFailedTests();
                IResultMap skippedTests = context.getSkippedTests();
                Collection<ITestNGMethod> disabledTests = context.getExcludedMethods();

                Element testSuite = reportDocument.createElement("testsuite");
                testSuite.setAttribute("name", context.getName());
                testSuite.setAttribute("tests", String.valueOf(context.getAllTestMethods().length));
                testSuite.setAttribute("errors", "0");
                testSuite.setAttribute("skipped", String.valueOf(skippedTests.size() + disabledTests.size()));
                testSuite.setAttribute("failures", String.valueOf(failedTests.size()));
                testSuite.setAttribute("timestamp", context.getStartDate().toString());
                testSuite.setAttribute("time", String.valueOf(context.getEndDate().getTime() - context.getStartDate().getTime()));
                testSuites.appendChild(testSuite);

                for (ITestResult testResult : failedTests.getAllResults()) {
                    Element testCase = createTestCaseElement(testResult);
                    testSuite.appendChild(testCase);
                    createFailureElement(testCase, testResult);
                }
                for (ITestResult testResult : skippedTests.getAllResults()) {
                    Element testCase = createTestCaseElement(testResult);
                    testSuite.appendChild(testCase);
                    testCase.appendChild(reportDocument.createElement("skipped"));
                }
                for (ITestNGMethod testNGMethod : disabledTests) {
                    Element testCase = createSkippedTestCaseElement(testNGMethod);
                    testSuite.appendChild(testCase);
                    testCase.appendChild(reportDocument.createElement("skipped"));
                }
                for (ITestResult testResult : passedTests.getAllResults()) {
                    Element testCase = createTestCaseElement(testResult);
                    testSuite.appendChild(testCase);
                }
            }
            writeReportToFile(outputDirectory);
        }
    }

    @SneakyThrows
    private void writeReportToFile(String outputDirectory) {
        FileOutputStream out = new FileOutputStream(outputDirectory + "/customReport.xml");
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(reportDocument), new StreamResult(out));
    }

    @SneakyThrows
    private Element createTestCaseElement(ITestResult testResult) {
        Element testCase = reportDocument.createElement("testcase");
        String testRailId = getTestRailId(testResult.getTestClass(), testResult.getMethod().getMethodName());
        setTestRailAttributes(testCase, testRailId);
        testCase.setAttribute("tafname", testResult.getName());
        testCase.setAttribute("tafclassname", testResult.getTestClass().getName());
        testCase.setAttribute("time", String.valueOf(testResult.getEndMillis() - testResult.getStartMillis()));
        return testCase;
    }

    @SneakyThrows
    private Element createSkippedTestCaseElement(ITestNGMethod testNGMethod) {
        Element testCase = reportDocument.createElement("testcase");
        String testRailId = getTestRailId(testNGMethod.getTestClass(), testNGMethod.getMethodName());
        setTestRailAttributes(testCase, testRailId);
        testCase.setAttribute("tafname", testNGMethod.getMethodName());
        testCase.setAttribute("tafclassname", testNGMethod.getTestClass().getName());
        return testCase;
    }

    private void createFailureElement(Element testCase, ITestResult testResult) {
        Throwable testResultThrowable = testResult.getThrowable();
        Element failureElement = reportDocument.createElement("failure");

        if (testResultThrowable != null) {
            failureElement.setAttribute("type", testResultThrowable.getClass().getName());
            String message = testResultThrowable.getMessage();
            if (message != null && message.length() > 0) {
                String formattedMessage = message.replace("\r", "").replace("\n", "");
                failureElement.setAttribute("message", formattedMessage);
            }
            String formattedStackTrace = Utils.shortStackTrace(testResultThrowable, false).replace("\r", "");
            failureElement.appendChild(reportDocument.createCDATASection(formattedStackTrace));
        }
        testCase.appendChild(failureElement);
    }

    @SneakyThrows
    private String getTestRailId(IClass testClass, String methodName) {
        String[] testRailId;
        Method testMethod = testClass.getRealClass().getMethod(methodName);

        if (testMethod.isAnnotationPresent(TestParameters.class)) {
            TestParameters testParametersAnnotation = testMethod.getAnnotation(TestParameters.class);
            testRailId = testParametersAnnotation.testRailId();
            return Arrays.stream(testRailId).iterator().next();
        } else {
            throw new IllegalArgumentException(String.format("TestRail ID missed for %s", methodName));
        }
    }

    private void setTestRailAttributes(Element testCase, String testRailId) {
        /*
        TestRail custom test case field automation_id = classname.name
         */
        testCase.setAttribute("name", testRailId);
        testCase.setAttribute("classname", "TA");
    }
}
