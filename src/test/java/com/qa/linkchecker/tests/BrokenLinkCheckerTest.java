package com.qa.linkchecker.tests;

import com.qa.linkchecker.model.LinkResult;
import com.qa.linkchecker.utils.ConfigReader;
import com.qa.linkchecker.utils.DriverManager;
import com.qa.linkchecker.utils.ExcelReader;
import com.qa.linkchecker.utils.ExcelReportWriter;
import com.qa.linkchecker.utils.LinkValidator;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * For every page listed in the input Excel file:
 *   1. Open the page with Selenium
 *   2. Collect every unique <a href> on the page
 *   3. Validate each link (HTTP status check)
 *   4. Record results, then write one consolidated Excel report at the end
 *
 * Runs one TestNG test method per page (data-driven), in parallel threads.
 */
public class BrokenLinkCheckerTest {

    private static List<String> pageList;
    private static final List<LinkResult> allResults = Collections.synchronizedList(new ArrayList<>());
    private Properties config;

    @BeforeSuite(alwaysRun = true)
    public void loadConfigAndPages() {
        config = ConfigReader.getConfig();
        String inputPath = config.getProperty("input.file.path");
        String sheetName = config.getProperty("input.sheet.name");
        String columnName = config.getProperty("input.column.name");

        pageList = ExcelReader.readPageList(inputPath, sheetName, columnName);

        if (pageList.isEmpty()) {
            throw new SkipException("No valid pages found in input Excel file: " + inputPath);
        }
        System.out.println("Loaded " + pageList.size() + " page(s) to check from " + inputPath);
    }

    @DataProvider(name = "pages", parallel = true)
    public Object[][] pageProvider() {
        Object[][] data = new Object[pageList.size()][1];
        for (int i = 0; i < pageList.size(); i++) {
            data[i][0] = pageList.get(i);
        }
        return data;
    }

    @BeforeMethod(alwaysRun = true)
    public void setupDriver() {
        config = ConfigReader.getConfig();
        DriverManager.initDriver(config);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownDriver() {
        DriverManager.quitDriver();
    }

    @Test(dataProvider = "pages", description = "Scan a page and validate every link found on it")
    public void checkLinksOnPage(String pageUrl) {
        WebDriver driver = DriverManager.getDriver();
        SoftAssert softAssert = new SoftAssert();
        List<LinkResult> pageResults = new ArrayList<>();
        boolean checkExternal = Boolean.parseBoolean(config.getProperty("check.external.links", "true"));

        try {
            driver.get(pageUrl);
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            List<WebElement> anchors = driver.findElements(By.tagName("a"));

            // Dedupe by href, preserve first-seen order
            Map<String, String> uniqueLinks = new LinkedHashMap<>();
            for (WebElement a : anchors) {
                String href = a.getAttribute("href");
                if (href == null || href.isBlank()) {
                    continue;
                }
                String text = a.getText();
                uniqueLinks.putIfAbsent(href, (text == null || text.isBlank()) ? "(no text)" : text.trim());
            }

            System.out.println("[" + pageUrl + "] " + uniqueLinks.size() + " unique link(s) found");

            for (Map.Entry<String, String> entry : uniqueLinks.entrySet()) {
                String linkUrl = entry.getKey();
                String linkText = entry.getValue();

                if (!checkExternal && isExternal(pageUrl, linkUrl)) {
                    pageResults.add(new LinkResult(pageUrl, linkText, linkUrl, -1, "SKIPPED",
                            "External link skipped (check.external.links=false)", 0));
                    continue;
                }

                LinkResult result = LinkValidator.validateLink(pageUrl, linkText, linkUrl, config);
                pageResults.add(result);

                if ("BROKEN".equals(result.getStatus()) || "ERROR".equals(result.getStatus())) {
                    softAssert.fail("Broken link on " + pageUrl + " -> " + linkUrl
                            + " [" + result.getStatusCode() + "] " + result.getRemarks());
                }
            }

        } catch (Exception e) {
            pageResults.add(new LinkResult(pageUrl, "(page load)", pageUrl, -1, "ERROR",
                    "Failed to load page: " + e.getMessage(), 0));
            softAssert.fail("Could not load page " + pageUrl + ": " + e.getMessage());
        } finally {
            allResults.addAll(pageResults);
        }

        softAssert.assertAll();
    }

    private boolean isExternal(String pageUrl, String linkUrl) {
        try {
            String pageHost = new URI(pageUrl).getHost();
            String linkHost = new URI(linkUrl).getHost();
            if (pageHost == null || linkHost == null) {
                return false;
            }
            return !pageHost.equalsIgnoreCase(linkHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @AfterSuite(alwaysRun = true)
    public void generateReport() {
        Properties cfg = ConfigReader.getConfig();
        String outputPath = cfg.getProperty("output.file.path");
        ExcelReportWriter.writeReport(outputPath, allResults);

        long brokenCount = allResults.stream()
                .filter(r -> "BROKEN".equals(r.getStatus()) || "ERROR".equals(r.getStatus()))
                .count();

        System.out.println("=================================================");
        System.out.println("Report generated: " + outputPath);
        System.out.println("Total links checked: " + allResults.size());
        System.out.println("Broken/Error links  : " + brokenCount);
        System.out.println("=================================================");
    }
}
