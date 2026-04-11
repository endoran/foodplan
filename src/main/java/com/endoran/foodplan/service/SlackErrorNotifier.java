package com.endoran.foodplan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SlackErrorNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackErrorNotifier.class);

    private final String token;
    private final String channel;
    private final HttpClient httpClient;

    public SlackErrorNotifier(
            @Value("${slack.error.token:}") String token,
            @Value("${slack.error.channel:}") String channel) {
        this.token = token;
        this.channel = channel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void notify(int status, String method, String path, String detail) {
        if (token.isBlank() || channel.isBlank()) {
            return;
        }

        String emoji = status >= 500 ? ":red_circle:" : ":warning:";
        String body = """
                {
                  "channel": "%s",
                  "blocks": [
                    {"type": "header", "text": {"type": "plain_text", "text": "Food Planner"}},
                    {"type": "section", "text": {"type": "mrkdwn", "text": "%s *%d* `%s %s`\\n%s"}}
                  ]
                }
                """.formatted(channel, emoji, status, method, path, escapeJson(detail));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://slack.com/api/chat.postMessage"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to send Slack error notification: {}", e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", " ");
    }
}
