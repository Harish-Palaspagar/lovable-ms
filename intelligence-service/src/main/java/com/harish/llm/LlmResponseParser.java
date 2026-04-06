package com.harish.llm;

import com.harish.entity.ChatEvent;
import com.harish.entity.ChatMessage;
import com.harish.enums.ChatEventStatus;
import com.harish.enums.ChatEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class LlmResponseParser {

    // Match <message>, <file>, <tool> tags with their content and closing tags
    // Using DOTALL so that . matches newlines as well
    private static final Pattern GENERIC_TAG_PATTERN = Pattern.compile(
            "(<(message|file|tool)([^>]*)>)(.*?)(</\\s*\\2\\s*>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(path|filePath|name|args)\\s*=\\s*(?:(['\"])(.*?)\\2|([^\\s>]+))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    public List<ChatEvent> parseChatEvents(String fullResponse, ChatMessage parentMessage) {

        List<ChatEvent> events = new ArrayList<>();

        if (fullResponse == null || fullResponse.isBlank()) {
            log.warn("parseChatEvents called with null/empty response");
            return events;
        }

        log.info("Parsing LLM response of length {} characters", fullResponse.length());

        int orderCounter = 1;
        Matcher matcher = GENERIC_TAG_PATTERN.matcher(fullResponse);

        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            String content = matcher.group(4).trim();

            log.debug("Found <{}> tag, attributes='{}', contentLength={}",
                    tagName, attributes, content.length());

            if (content.startsWith("<![CDATA[") && content.endsWith("]]>")) {
                content = content.substring(9, content.length() - 3).trim();
            }

            // Strip markdown code fences if the LLM wrapped the content
            // Using greedy .* to match up to the last ``` in case of inner code blocks.
            // Also it skips any chatty text before the first ``` and after the last ```.
            Matcher mdMatcher = Pattern.compile("```[a-zA-Z]*\\s*\\n(.*)```", Pattern.DOTALL).matcher(content);
            if (mdMatcher.find()) {
                content = mdMatcher.group(1).trim();
            } else {
                content = content.trim();
            }

            Map<String, String> attrMap = extractAttributes(attributes);
            ChatEvent.ChatEventBuilder builder = ChatEvent.builder()
                    .status(ChatEventStatus.CONFIRMED)
                    .chatMessage(parentMessage)
                    .content(content)
                    .sequenceOrder(orderCounter++);

            switch (tagName) {
                case "message" -> builder.type(ChatEventType.MESSAGE);
                case "file" -> {
                    builder.type(ChatEventType.FILE_EDIT);
                    builder.status(ChatEventStatus.PENDING);
                    String filePath = resolveFilePath(attrMap);
                    builder.filePath(filePath);
                    log.info("Parsed FILE_EDIT: path='{}', contentLength={}", filePath, content.length());
                    if (filePath == null || filePath.isBlank()) {
                        log.error("FILE_EDIT tag has no valid path! attributes='{}', attrMap={}",
                                attributes, attrMap);
                    }
                }
                case "tool" -> {
                    builder.type(ChatEventType.TOOL_LOG);
                    builder.metadata(attrMap.get("args"));
                }
                default -> { continue; }
            }
            events.add(builder.build());
        }

        log.info("Parser extracted {} events total from LLM response", events.size());

        if (events.isEmpty() && fullResponse.length() > 100) {
            log.warn("Parser found NO events in response of length {}. " +
                     "First 300 chars: {}", fullResponse.length(),
                    fullResponse.substring(0, Math.min(300, fullResponse.length())));
        }

        return events;

    }

    private Map<String, String> extractAttributes(String attributeString) {

        Map<String, String> attributes = new HashMap<>();
        if (attributeString == null) return attributes;
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributeString);
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            attributes.put(key, value.trim());
            log.debug("Extracted attribute: {}='{}'", key, value);
        }
        return attributes;

    }

    private String resolveFilePath(Map<String, String> attributes) {

        String path = attributes.get("path");
        if (path != null && !path.isBlank()) {
            return path.trim();
        }
        String filePath = attributes.get("filepath");
        if (filePath != null && !filePath.isBlank()) {
            return filePath.trim();
        }
        String name = attributes.get("name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return null;

    }

}
