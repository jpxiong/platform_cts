/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.webkit.cts;

import android.test.ActivityInstrumentationTestCase2;
import android.webkit.webdriver.By;
import android.webkit.webdriver.WebDriver;
import android.webkit.webdriver.WebElement;
import android.webkit.webdriver.WebElementNotFoundException;
import android.webkit.webdriver.WebElementStaleException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.webkit.cts.TestHtmlConstants.FORM_PAGE_URL;
import static android.webkit.cts.TestHtmlConstants.HELLO_WORLD_URL;

/**
 * Tests for {@link android.webkit.webdriver.WebDriver}.
 */
public class WebDriverTest extends
        ActivityInstrumentationTestCase2<WebDriverStubActivity>{
    private WebDriver mDriver;
    private CtsTestServer mWebServer;
    private static final String SOME_TEXT = "Some text";
    private static final String DIV_TEXT = "A div Nested text";
    private static final String NESTED_TEXT = "Nested text";
    private static final String DIV_ID = "divId";
    private static final String SOME_TEXT_ID = "someTextId";
    private static final String BAD_ID = "BadId";
    private static final String NESTED_LINK_ID = "nestedLinkId";

    public WebDriverTest() {
        super(WebDriverStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDriver = getActivity().getDriver();
        mWebServer = new CtsTestServer(getActivity(), false);
    }

    @Override
    protected void tearDown() throws Exception {
        mWebServer.shutdown();
        super.tearDown();
    }

    public void testGetIsBlocking() {
        mDriver.get(mWebServer.getDelayedAssetUrl(HELLO_WORLD_URL));
        assertTrue(mDriver.getPageSource().contains("hello world!"));
    }

    // By id
    public void testFindElementById() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.id(SOME_TEXT_ID));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.id(DIV_ID));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByIdThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.id(BAD_ID));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementById() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.id(NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    // By linkText
    public void testFindElementByLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.id(SOME_TEXT_ID));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.id(DIV_ID));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByLinkTextThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.id(BAD_ID));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.id(NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    // By partialLinkText
    public void testFindElementByPartialLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.id(SOME_TEXT_ID));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.id(DIV_ID));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByPartialLinkTextThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.id(BAD_ID));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByPartialLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.id(NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    // by name
    public void testFindElementByName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.id(SOME_TEXT_ID));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.id(DIV_ID));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByNameThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.id(BAD_ID));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.id(NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    // By tagName
    public void testFindElementByTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.id(SOME_TEXT_ID));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.id(DIV_ID));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByTagNameThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.id(BAD_ID));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.id(NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    // By xpath
    public void testFindElementByXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.id(SOME_TEXT_ID));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.id(DIV_ID));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByXPathThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.id(BAD_ID));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.id(NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testGetTextThrowsIfElementIsStale() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElement(By.id(DIV_ID));
        mDriver.get(mWebServer.getAssetUrl(HELLO_WORLD_URL));
        try {
            div.getText();
            fail("This should have failed.");
        } catch (WebElementStaleException e) {
            // This is expected
        }
    }

    public void testExecuteScriptShouldReturnAString() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Object result = mDriver.executeScript("return document.title");
        assertEquals("Test Page", (String) result);
    }

    public void testExecuteScriptShouldReturnAWebElement() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Object result = mDriver.executeScript(
                "return document.getElementsByTagName('div')[0];");
        assertTrue(result instanceof WebElement);
        assertEquals(DIV_TEXT, ((WebElement) result).getText());
    }

    public void testExecuteScriptShouldPassAndReturnADouble() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Double expected = new Double(1.2);
        Object result = mDriver.executeScript("return arguments[0];", expected);
        assertTrue(result instanceof Double);
        assertEquals(expected, (Double) result);
    }

    public void testExecuteScriptShouldPassAndReturnALong() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Long expected = new Long(1);
        Object result = mDriver.executeScript("return arguments[0];", expected);
        assertTrue(result instanceof Long);
        assertEquals(expected, (Long) result);
    }

    public void testExecuteScriptShouldPassReturnABoolean() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Boolean expected = new Boolean(true);
        Object result = mDriver.executeScript("return arguments[0] === true;",
                expected);
        assertTrue(result instanceof Boolean);
        assertEquals(expected, (Boolean) result);
    }

    public void testExecuteScriptShouldReturnAList() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<String> expected = new ArrayList();
        expected.add("one");
        expected.add("two");
        expected.add("three");

        Object result = mDriver.executeScript(
                "return ['one', 'two', 'three'];");

        assertTrue(expected.equals((List<String>) result));
    }

    public void testExecuteScriptShouldReturnNestedList() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<Object> expected = new ArrayList();
        expected.add("one");
        List<Object> nestedList = new ArrayList();
        nestedList.add(true);
        nestedList.add(false);
        expected.add(nestedList);
        Map<String, Object> nestedMap = new HashMap();
        nestedMap.put("bread", "cheese");
        nestedMap.put("hungry", true);
        expected.add(nestedMap);

        Object result = mDriver.executeScript(
                "return ['one', [true, false], "
                + "{bread:'cheese', hungry:true}];");

        assertTrue(expected.equals(result));
    }

    public void testExecuteScriptShouldBeAbleToReturnALisOfwebElements() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> result = (List<WebElement>) mDriver.executeScript(
                "return document.getElementsByTagName('a')");
        assertEquals(5, result.size());
    }

    public void testExecuteScriptShouldReturnAMap() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Map<String, Object> expected = new HashMap();
        expected.put("abc", Long.valueOf(123));
        expected.put("cat", false);
        Map<String, Object> nestedMap = new HashMap();
        nestedMap.put("bread", "cheese");
        nestedMap.put("hungry", true);
        expected.put("map", nestedMap);
        List<String> nestedList = new ArrayList();
        nestedList.add("bou");
        nestedList.add("truc");
        expected.put("list", nestedList);

        Object res = mDriver.executeScript("return {abc:123, cat:false, "
                + "map:{bread:'cheese', hungry:true}, list:['bou', 'truc']};");
        assertTrue(res instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res;
        assertEquals(expected.size(), result.size());

        assertTrue(expected.equals(result));
    }

    public void testExecuteScriptShouldThrowIfJsIsBad() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.executeScript("return bou();");
            fail("This should have failed");
        } catch (RuntimeException e) {
            // This is expected.
        }
    }

    public void testExecuteScriptShouldbeAbleToPassAString() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        String expected = "bou";
        Object result = mDriver.executeScript("return arguments[0]", expected);
        assertEquals(expected, (String) result);
    }

    public void testExecuteScriptShouldBeAbleToPassWebElement() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElement(By.id(DIV_ID));
        Object result = mDriver.executeScript(
                "arguments[0]['flibble'] = arguments[0].getAttribute('id');"
                + "return arguments[0]['flibble'];", div);
        assertEquals(DIV_ID, (String) result);
    }

    public void testExecuteScriptShouldBeAbleToPassAList() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<String> expected = new ArrayList();
        expected.add("apple");
        expected.add("cheese");
        expected.add("food");

        Object result = mDriver.executeScript(
                "return arguments[0].length", expected);
        assertEquals(expected.size(), ((Long) result).intValue());
    }

    public void testExecuteScriptShouldBeAbleToPassNestedLists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<Object> expected = new ArrayList();
        expected.add("apple");
        expected.add("cheese");
        List<Integer> nested = new ArrayList();
        nested.add(1);
        nested.add(2);
        expected.add(nested);
        expected.add("food");

        Object result = mDriver.executeScript(
                "return arguments[0][2].length", expected);
        assertEquals(nested.size(), ((Long) result).intValue());
    }

    public void testExecuteScriptShouldBeAbleToPassAMap() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Map<String, String> expected = new HashMap();
        expected.put("apple", "pie");
        expected.put("cheese", "cake");

        Object result = mDriver.executeScript(
                "return arguments[0].apple", expected);
        assertEquals(expected.get("apple"), (String) result);
    }

    public void testExecuteScriptShouldBeAbleToPassNestedMaps() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Map<String, Object> expected = new HashMap();
        expected.put("apple", "pie");
        Map<String, String> nested = new HashMap();
        nested.put("foo", "boo");
        expected.put("nested", nested);
        expected.put("cheese", "cake");

        Object result = mDriver.executeScript(
                "return arguments[0].nested.foo", expected);
        assertEquals(((Map<String, Object>)expected.get("nested")).get("foo"),
                (String) result);
    }


    public void testExecuteScriptShouldThrowIfArgumentIsNotValid() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.executeScript("return arguments[0];", mDriver);
            fail("This should have failed");
        } catch (RuntimeException e) {
            // This is expected.
        }
    }

    public void testExecuteScriptHandlesStringCorrectly() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        String cheese = "cheese";
        String bread = "bread";
        Object result = mDriver.executeScript(
                "return 'I love ' + arguments[0] + ' and ' + arguments[1]",
                cheese, bread);
        assertEquals("I love cheese and bread", (String) result);
    }

    public void testExecuteScriptShouldThrowIfNoPageLoaded() {
        try {
            mDriver.executeScript("return 'bou';");
            fail("This should have failed");
        } catch (RuntimeException e) {
            // This is expected.
        }
    }

    public void testExecuteScriptShouldBeAbleToCreatePersistentValue() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        mDriver.executeScript("document.bidule = ['hello']");
        Object result = mDriver.executeScript(
                "return document.bidule.shift();");
        assertEquals("hello", (String) result);
    }

    public void testExecuteScriptEscapesQuotesAndBackslash() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        assertTrue((Boolean) mDriver.executeScript(
                "return \"foo'\\\"\" == arguments[0];", "foo'\""));
        assertTrue((Boolean) mDriver.executeScript(
                "return \"foo'\\\"bar\" == arguments[0];", "foo'\"bar"));
        assertTrue((Boolean) mDriver.executeScript(
                "return 'foo\"' == arguments[0];", "foo\""));
        assertTrue((Boolean) mDriver.executeScript(
                "return \"foo'\" == arguments[0];", "foo'"));
        assertTrue((Boolean) mDriver.executeScript(
                "return \"foo\\\\\\\"\" == arguments[0];", "foo\\\""));
        assertTrue((Boolean) mDriver.executeScript(
                "return \"f\\\"o\\\\o\\\\\\\\\\\"\" == arguments[0];",
                "f\"o\\o\\\\\""));
    }
}
