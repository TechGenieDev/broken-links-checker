package com.qa.linkchecker.model;

/**
 * Holds the outcome of validating a single link found on a page.
 */
public class LinkResult {

    private final String sourcePage;
    private final String linkText;
    private final String linkUrl;
    private final int statusCode;
    private final String status;       // OK, BROKEN, REDIRECT, SKIPPED, ERROR
    private final String remarks;
    private final long responseTimeMs;

    public LinkResult(String sourcePage, String linkText, String linkUrl,
                       int statusCode, String status, String remarks, long responseTimeMs) {
        this.sourcePage = sourcePage;
        this.linkText = linkText;
        this.linkUrl = linkUrl;
        this.statusCode = statusCode;
        this.status = status;
        this.remarks = remarks;
        this.responseTimeMs = responseTimeMs;
    }

    public String getSourcePage() {
        return sourcePage;
    }

    public String getLinkText() {
        return linkText;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }
}
