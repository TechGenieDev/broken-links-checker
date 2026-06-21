# Broken Link Checker — Java + Selenium + TestNG

Reads a list of page URLs from an input Excel file, opens each page with Selenium,
collects every link on the page, checks each link's HTTP status, and writes a
color-coded Excel report (`Summary` + `Link Details` sheets).

## Project Structure

```
broken-link-checker/
├── pom.xml
├── input/
│   └── pages_to_check.xlsx          ← sample input (replace with your own list)
├── reports/
│   └── BrokenLinkReport.xlsx        ← generated after each run
└── src/
    ├── main/java/com/qa/linkchecker/
    │   ├── model/
    │   │   └── LinkResult.java       (POJO holding one link's check result)
    │   └── utils/
    │       ├── ConfigReader.java     (loads config.properties)
    │       ├── ExcelReader.java      (reads page list from input Excel)
    │       ├── ExcelReportWriter.java(writes the formatted output Excel)
    │       ├── LinkValidator.java    (HTTP HEAD/GET status check per link)
    │       └── DriverManager.java    (thread-safe ChromeDriver factory)
    └── test/
        ├── java/com/qa/linkchecker/tests/
        │   └── BrokenLinkCheckerTest.java   (TestNG test - one method per page)
        └── resources/
            ├── config.properties
            └── testng.xml
```

## Prerequisites

- JDK 17+
- Maven 3.8+
- Google Chrome installed (ChromeDriver is auto-managed by WebDriverManager —
  no manual driver download needed)

> Note: this project was generated in a sandbox without access to Maven Central,
> so the build has not been compiled/run here. Run `mvn clean test` on your own
> machine to execute it — that's the standard way to verify a Maven project.

## 1. Prepare your input Excel file

Replace `input/pages_to_check.xlsx` with your own file (or edit the sample). It
must have:
- A header row
- A column named **URL** (configurable, see below) containing one full page
  URL per row (e.g. `https://yoursite.com/products`)

A 3-row sample is already included so you can do a first test run immediately.

## 2. Configure (`src/test/resources/config.properties`)

| Property | Meaning |
|---|---|
| `input.file.path` | Path to the input Excel file |
| `input.sheet.name` | Sheet name to read from |
| `input.column.name` | Header text of the column containing URLs |
| `output.file.path` | Where the report Excel is written |
| `headless` | `true` to run Chrome headless (recommended for CI) |
| `page.load.timeout.seconds` | Selenium page load timeout |
| `link.connect.timeout.ms` / `link.read.timeout.ms` | Timeouts for each link check |
| `treat.redirect.as.broken` | If `true`, 3xx links are reported as BROKEN instead of REDIRECT |
| `check.external.links` | If `false`, only links on the same domain as the page are checked |
| `thread.count` | Must match `thread-count` in `testng.xml` |

To run more/fewer pages in parallel, update **both**:
- `thread.count` in `config.properties`
- `thread-count="3"` in `src/test/resources/testng.xml`

## 3. Run

```bash
mvn clean test
```

This will:
1. Read every URL from `input/pages_to_check.xlsx`
2. Launch Chrome (headless by default) and visit each page
3. Extract every unique link on the page, validate it, record the result
4. After all pages finish, write `reports/BrokenLinkReport.xlsx`

TestNG will also mark each page's test method as **passed/failed** based on
whether broken links were found on it — useful for CI pipelines and the
`target/surefire-reports` HTML report, in addition to the Excel report.

## 4. Reading the report

**Summary sheet** — total links checked, and counts of OK / Broken / Redirect /
Skipped / Error.

**Link Details sheet** — one row per link, color-coded:
- 🟩 Green = OK (2xx)
- 🟥 Red = Broken (4xx/5xx)
- 🟨 Yellow = Redirect (3xx)
- 🟧 Orange = Error (timeout, unknown host, page failed to load, etc.)
- ⬜ Grey = Skipped (mailto:, tel:, javascript:, anchor-only, or external link
  excluded by config)

Columns: Source Page, Link Text, Link URL, Status Code, Status, Response Time
(ms), Remarks. The sheet has a frozen header row and auto-filter enabled.

## How link checking works

For each page, Selenium collects all `<a href>` elements (de-duplicated). Each
link is then validated with a plain `HttpURLConnection` HEAD request (falling
back to GET if the server returns 403/405) — this is much faster than loading
every single link in a real browser. A realistic `User-Agent` header is sent
to avoid false positives from bot-blocking.

## Extending

- **Firefox/Edge support**: add a `browser` switch in `DriverManager.initDriver()`.
- **Email the report**: hook in JavaMail after `generateReport()` in the test class.
- **Retry flaky links**: wrap `LinkValidator.validateLink()` with a small retry
  loop for ERROR results before marking final status.
- **CI integration**: `mvn clean test` returns a non-zero exit code if any page
  had broken links (via the SoftAssert failures), so it plugs straight into a
  Jenkins/GitHub Actions pipeline.
