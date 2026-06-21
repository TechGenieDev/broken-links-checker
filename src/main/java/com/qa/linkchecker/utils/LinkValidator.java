package com.qa.linkchecker.utils;

import com.qa.linkchecker.model.LinkResult;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Checks whether a single link is reachable, without using the browser
 * (much faster than loading every link in Selenium).
 */
public class LinkValidator {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    public static LinkResult validateLink(String sourcePage, String linkText, String linkUrl, Properties config) {

        if (isSkippable(linkUrl)) {
            return new LinkResult(sourcePage, linkText, linkUrl, -1, "SKIPPED",
                    "Non-HTTP scheme or empty link", 0);
        }

        int connectTimeout = Integer.parseInt(config.getProperty("link.connect.timeout.ms", "8000"));
        int readTimeout = Integer.parseInt(config.getProperty("link.read.timeout.ms", "8000"));
        boolean redirectIsBroken = Boolean.parseBoolean(config.getProperty("treat.redirect.as.broken", "false"));

        long start = System.currentTimeMillis();
        try {
            int statusCode = sendRequest(linkUrl, "HEAD", connectTimeout, readTimeout);

            // Some servers don't support HEAD (405) or block it (403) - retry with GET
            if (statusCode == 405 || statusCode == 403 || statusCode == -1) {
                statusCode = sendRequest(linkUrl, "GET", connectTimeout, readTimeout);
            }

            long elapsed = System.currentTimeMillis() - start;

            if (statusCode >= 200 && statusCode < 300) {
                return new LinkResult(sourcePage, linkText, linkUrl, statusCode, "OK", "", elapsed);
            } else if (statusCode >= 300 && statusCode < 400) {
                String status = redirectIsBroken ? "BROKEN" : "REDIRECT";
                return new LinkResult(sourcePage, linkText, linkUrl, statusCode, status, "Redirected", elapsed);
            } else {
                return new LinkResult(sourcePage, linkText, linkUrl, statusCode, "BROKEN", "HTTP error", elapsed);
            }

        } catch (UnknownHostException e) {
            return new LinkResult(sourcePage, linkText, linkUrl, -1, "ERROR",
                    "Unknown host: " + e.getMessage(), System.currentTimeMillis() - start);
        } catch (SocketTimeoutException e) {
            return new LinkResult(sourcePage, linkText, linkUrl, -1, "ERROR",
                    "Timed out", System.currentTimeMillis() - start);
        } catch (IOException e) {
            return new LinkResult(sourcePage, linkText, linkUrl, -1, "ERROR",
                    e.getClass().getSimpleName() + ": " + e.getMessage(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            return new LinkResult(sourcePage, linkText, linkUrl, -1, "ERROR",
                    "Unexpected error: " + e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    private static int sendRequest(String urlStr, String method, int connectTimeout, int readTimeout)
            throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();
            return connection.getResponseCode();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isSkippable(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("mailto:")
                || lower.startsWith("tel:")
                || lower.startsWith("javascript:")
                || lower.startsWith("#");
    }
}
