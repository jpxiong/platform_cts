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
import android.webkit.webdriver.WebDriverException;
import android.webkit.webdriver.WebElement;
import android.webkit.webdriver.WebElementNotFoundException;

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
    private static final String DIV_TEXT =
            "A div Nested text a nested link Foo Nested text";
    private static final String NESTED_TEXT = "Nested text";
    private static final String DIV_ID = "divId";
    private static final String SOME_TEXT_ID = "someTextId";
    private static final String BAD_ID = "BadId";
    private static final String NESTED_LINK_ID = "nestedLinkId";
    private static final String FIRST_DIV = "firstDiv";
    private static final String INEXISTENT = "inexistent";
    private static final String ID = "id";
    private static final String OUTTER = "outter";

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

    // Navigation
    public void testNavigateBack() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.id("inner"));
        link.click();
        assertEquals("test hello world", mDriver.getTitle());
        mDriver.navigate().back();
        assertEquals("Form Test Page", mDriver.getTitle());
    }

    public void testNavigateForward() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.id("inner"));
        link.click();
        mDriver.navigate().back();
        assertEquals("Form Test Page", mDriver.getTitle());
        mDriver.navigate().forward();
        assertEquals("test hello world", mDriver.getTitle());
    }

    public void testRefresh() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Long result = (Long) mDriver.executeScript(
                "document.bou = 1;return document.bou;");
        assertEquals(1, result.intValue());
        mDriver.navigate().refresh();
        String result2 = (String) mDriver.executeScript("return document.bou;");
        assertNull(result2);
    }

    // getText
    public void testGetTextReturnsEmptyString() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement emptyLink = mDriver.findElement(By.id("emptyLink"));
        assertEquals("", emptyLink.getText());
    }

    // getAttribute
    public void testGetValidAttribute() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.linkText("Link=equalssign"));
        assertEquals("foo", link.getAttribute("href"));
    }

    public void testGetInvalidAttributeReturnsNull() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.linkText("Link=equalssign"));
        assertNull(link.getAttribute(INEXISTENT));
    }

    public void testGetAttributeNotSetReturnsNull() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.linkText("Link=equalssign"));
        assertNull(link.getAttribute(INEXISTENT));
    }

    // getTagName
    public void testTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement span = mDriver.findElement(By.tagName("span"));
        assertEquals("SPAN", span.getTagName());
    }

    // isEnabled
    public void testIsEnabled() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElement(By.id(DIV_ID));
        assertTrue(div.isEnabled());

        WebElement input = mDriver.findElement(By.name("inputDisabled"));
        assertFalse(input.isEnabled());
    }

    // isSelected
    public void testIsSelected() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement optionOne = mDriver.findElement(By.id("one"));
        assertTrue(optionOne.isSelected());

        WebElement optionTwo = mDriver.findElement(By.id("two"));
        assertFalse(optionTwo.isSelected());

        WebElement selectEggs = mDriver.findElement(By.id("eggs"));
        assertTrue(selectEggs.isSelected());

        WebElement selectHam = mDriver.findElement(By.id("ham"));
        assertFalse(selectHam.isSelected());

        WebElement inputCheese = mDriver.findElement(By.id("cheese"));
        assertFalse(inputCheese.isSelected());

        WebElement inputCheesePeas = mDriver.findElement(
                By.id("cheese_and_peas"));
        assertTrue(inputCheesePeas.isSelected());
    }

    public void testIsSelectedOnHiddenInputThrows() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement inputHidden = mDriver.findElement(By.name("hidden"));
        try {
            inputHidden.isSelected();
            fail();
        } catch (WebDriverException e) {
            // This is expcted
        }
    }

    public void testIsSelectedOnNonSelectableElementThrows() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link= mDriver.findElement(By.linkText("Foo"));
        try {
            link.isSelected();
            fail();
        } catch (WebDriverException e) {
            // This is expected
        }
    }

    // toogle
    public void testToggleCheckbox() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement check = mDriver.findElement(By.id("checky"));
        assertFalse(check.isSelected());
        assertTrue(check.toggle());
        assertFalse(check.toggle());
    }

    public void testToggleOnNonTogglableElements() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement inputHidden = mDriver.findElement(By.name("hidden"));
        try {
            inputHidden.toggle();
            fail();
        } catch (WebDriverException e) {
            // This is expected
        }
    }

    // getCssValue
    public void testGetCssValue() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement text = mDriver.findElement(By.name("textinput"));
        assertEquals("red", text.getCssValue("background-color"));
    }

    public void testGetCssValueReturnsNullWhenPropertyNotFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement text = mDriver.findElement(By.name("textinput"));
        assertNull(text.getCssValue(INEXISTENT));
    }

    // getSize
    public void testGetSize() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement text = mDriver.findElements(By.name("textinput")).get(0);
        assertEquals(100, text.getSize().x);
        assertEquals(50, text.getSize().y);
    }

    // getLocation
    public void testGetLocation () {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement tag = mDriver.findElement(By.linkText("Tag A"));
        assertEquals(8, tag.getLocation().x);
        assertEquals(8, tag.getLocation().y);
    }

    // isDisplayed
    public void testIsDisplayed() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement hidden = mDriver.findElement(By.id("fromage"));
        assertFalse(hidden.isDisplayed());
        WebElement checky = mDriver.findElement(By.id("checky"));
        assertTrue(checky.isDisplayed());
    }

    // click
    public void testClickOnWebElement() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.id("inner"));
        link.click();
        assertEquals("test hello world", mDriver.getTitle());
    }

    // sendKeys
    public void testSendKeys() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement type = mDriver.findElement(By.id("typehere"));
        assertEquals("", type.getAttribute("value"));
        type.sendKeys("hello");

        assertEquals("hello", type.getAttribute("value"));
        type.sendKeys(" ", "world", "!");
        assertEquals("hello world!", type.getAttribute("value"));
    }

    // submit
    public void testSubmit() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement link = mDriver.findElement(By.id("fromage"));
        link.submit();
        assertEquals("test hello world", mDriver.getTitle());
    }

    // clear
    public void testClear() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement type = mDriver.findElement(By.id("typehere"));
        assertEquals("", type.getAttribute("value"));
        type.sendKeys("hello");
        assertEquals("hello", type.getAttribute("value"));
        type.clear();
        assertEquals("", type.getAttribute("value"));
    }

    // findElement
    public void testFindElementThrowsIfNoPageIsLoaded() {
        try {
            mDriver.findElement(By.id(SOME_TEXT_ID));
            fail();
        } catch (WebDriverException e) {
            // this is expected
        }
    }

    // findElements
    public void testFindElementsThrowsIfNoPageIsLoaded() {
        try {
            mDriver.findElements(By.id(SOME_TEXT_ID));
            fail();
        } catch (WebDriverException e) {
            // this is expected
        }
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

    public void testFindElementsById() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.id(ID + "3"));
        assertEquals(2, elements.size());
        assertEquals("A paragraph", elements.get(1).getText());
    }

    public void testFindElementsByIdReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.id(INEXISTENT));
        assertNotNull(elements);
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsById() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElement(By.name(FIRST_DIV));
        List<WebElement> elements = div.findElements(By.id("n1"));
        assertEquals(2, elements.size());
        assertEquals("spann1", elements.get(1).getAttribute("name"));
    }

    // By linkText
    public void testFindElementByLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.linkText("Nested text"));
        assertTrue(NESTED_LINK_ID.equals(element.getAttribute(ID)));
    }

    public void testFindElementByLinkTextThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.linkText(INEXISTENT));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.linkText("Foo"));
        assertTrue("inner".equals(nestedNode.getAttribute(ID)));
    }

    public void testFindElementsByLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.linkText("Foo"));
        assertEquals(4, elements.size());
    }

    public void testFindElementsByLinkTextReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.linkText("Boo"));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElement(By.name(FIRST_DIV));
        List<WebElement> elements =
                div.findElements(By.linkText("Nested text"));
        assertEquals(2, elements.size());
    }

    // By partialLinkText
    public void testFindElementByPartialLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.partialLinkText("text"));
        assertTrue(SOME_TEXT.equals(element.getText()));
    }

    public void testFindElementByPartialLinkTextThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.partialLinkText(INEXISTENT));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByPartialLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.partialLinkText("text"));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testFindElementsByPartialLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements =
                mDriver.findElements(By.partialLinkText("text"));
        assertTrue(elements.size() > 2);
    }

    public void
    testFindElementsByPartialLinkTextReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements =
                mDriver.findElements(By.partialLinkText(INEXISTENT));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByPartialLinkText() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElements(By.name(FIRST_DIV)).get(0);
        List<WebElement> elements =
                div.findElements(By.partialLinkText("text"));
        assertEquals(2, elements.size());
    }

    // by name
    public void testFindElementByName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.name("foo"));
        assertTrue(OUTTER.equals(element.getAttribute(ID)));
    }

    public void testFindElementByNameThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.name(INEXISTENT));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.name("nestedLink"));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testFindElementsByName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.name("text"));
        assertEquals(2, elements.size());
    }

    public void testFindElementsByNameReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.name(INEXISTENT));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElements(By.xpath(
                "//div[@" + ID + "='divId']"))
                .get(0);
        List<WebElement> elements = div.findElements(By.name("foo"));
        assertEquals(1, elements.size());
    }

    // By tagName
    public void testFindElementByTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.tagName("a"));
        assertTrue("Tag A".equals(element.getText()));
    }

    public void testFindElementByTagNameThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.tagName(INEXISTENT));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.tagName("a"));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testFindElementsByTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.tagName("a"));
        assertTrue(elements.size() > 0);
    }

    public void testFindElementsByTagNameReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(
                By.tagName(INEXISTENT));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByTagName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElement(By.xpath(
                "//div[@" + ID + "='divId']"));
        List<WebElement> elements = div.findElements(By.tagName("span"));
        assertEquals(1, elements.size());
    }

    // By xpath
    public void testFindElementByXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element =
                mDriver.findElement(By.xpath(
                "//a[@" + ID + "=\"someTextId\"]"));
        assertTrue(SOME_TEXT.equals(element.getText()));

        element = mDriver.findElement(By.xpath("//div[@name='firstDiv']"));
        assertTrue(DIV_TEXT.equals(element.getText()));
    }

    public void testFindElementByXPathThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.xpath("//a[@" + ID + "='inexistant']"));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.xpath(
                "//div[@" + ID + "='divId']"));
        WebElement nestedNode = parent.findElement(
                By.xpath(".//a[@" + ID + "='nestedLinkId']"));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testFindElementsByXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(
                By.xpath("//a[@name='foo']"));
        assertTrue(elements.size() > 1);
    }

    public void testFindElementsByXPathReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements =
                mDriver.findElements(By.xpath(
                        "//a[@" + ID + "='inexistant']"));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement div = mDriver.findElements(By.xpath(
                "//div[@" + ID + "='divId']"))
                .get(0);
        List<WebElement> elements = div.findElements(
                By.xpath(".//a[@name='foo']"));
        assertEquals(1, elements.size());
    }

    public void testFindElementByXpathWithInvalidXPath() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.xpath("//a@" + ID + "=inexistant']"));
            fail("This should have failed.");
        } catch (WebDriverException e) {
            // This is expected
        }
    }

    // By className
    public void testFindElementByClassName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.className(" spaceAround "));
        assertTrue("Spaced out".equals(element.getText()));
    }

    public void testFindElementByClassNameThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.className("bou"));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByClassName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(By.className("divClass"));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testFindElementsByClassName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements =
                mDriver.findElements(By.className("divClass"));
        assertTrue(elements.size() > 1);
    }

    public void testFindElementsByClassNameReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements =
                mDriver.findElements(By.className(INEXISTENT));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByClassName() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        List<WebElement> nested =
                parent.findElements(By.className("divClass"));
        assertTrue(nested.size() > 0);
    }

    // By css
    public void testFindElementByCss() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement element = mDriver.findElement(By.css("#" + "outter"));
        assertTrue("Foo".equals(element.getText()));
    }

    public void testFindElementByCssThrowsIfElementDoesNotExists() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        try {
            mDriver.findElement(By.css("bou.foo"));
            fail("This should have failed.");
        } catch (WebElementNotFoundException e) {
            // This is expected
        }
    }

    public void testFindNestedElementByCss() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        WebElement nestedNode = parent.findElement(
                By.css("#" + NESTED_LINK_ID));
        assertTrue(NESTED_TEXT.equals(nestedNode.getText()));
    }

    public void testFindElementsByCss() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(
                By.css("#" + SOME_TEXT_ID));
        assertTrue(elements.size() > 0);
    }

    public void testFindElementsByCssReturnsEmptyListIfNoResultsFound() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        List<WebElement> elements = mDriver.findElements(By.css("bou.foo"));
        assertEquals(0, elements.size());
    }

    public void testFindNestedElementsByCss() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        WebElement parent = mDriver.findElement(By.id(DIV_ID));
        List<WebElement> nested = parent.findElements(
                By.css("#" + NESTED_LINK_ID));
        assertEquals(1, nested.size());
    }

    public void testExecuteScriptShouldReturnAString() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Object result = mDriver.executeScript("return document.title");
        assertEquals("Form Test Page", (String) result);
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
        assertTrue(result.size() > 1);
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
        } catch (WebDriverException e) {
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
                "arguments[0]['flibble'] = arguments[0].getAttribute('"
                + ID + "');"
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
        } catch (IllegalArgumentException e) {
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
        } catch (WebDriverException e) {
            // This is expected.
        }
    }

    public void testExecuteScriptShouldBeAbleToCreatePersistentValue() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        mDriver.executeScript("document.b" + ID + "ule = ['hello']");
        Object result = mDriver.executeScript(
                "return document.b" + ID + "ule.shift();");
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

    public void testExecuteScriptReturnsNull() {
        mDriver.get(mWebServer.getAssetUrl(FORM_PAGE_URL));
        Object result = mDriver.executeScript("return null;");
        assertNull(result);
        result = mDriver.executeScript("return undefined;");
        assertNull(result);
    }

    public void testExecuteScriptShouldThrowIfNoPageIsLoaded() {
        try {
            Object result = mDriver.executeScript("return null;");
            fail();
        } catch (Exception e) {

        }
    }
}
