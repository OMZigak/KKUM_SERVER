package org.kkumulkkum.server.log.discord;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.micrometer.core.instrument.util.StringEscapeUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kkumulkkum.server.exception.BusinessException;
import org.kkumulkkum.server.exception.code.BusinessErrorCode;
import org.kkumulkkum.server.log.model.EmbedObject;
import org.kkumulkkum.server.log.util.MDCUtil;
import org.kkumulkkum.server.log.util.StringUtil;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Setter
public class DiscordAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private String discordWebhookUrl;
    private String username;
    private String avatarUrl;

    private static Color getLevelColor(ILoggingEvent eventObject) {
        String level = eventObject.getLevel().levelStr;
        if (level.equals("WARN")) {
            return Color.yellow;
        } else if (level.equals("ERROR")) {
            return Color.red;
        }
        return Color.blue;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        DiscordWebHook discordWebhook = new DiscordWebHook(discordWebhookUrl, username, avatarUrl, false);
        Map<String, String> mdcPropertyMap = eventObject.getMDCPropertyMap();
        Color messageColor = getLevelColor(eventObject);

        String level = eventObject.getLevel().levelStr;
        String exceptionBrief = "";
        IThrowableProxy throwable = eventObject.getThrowableProxy();

        if (throwable != null) {
            exceptionBrief = throwable.getClassName() + ": " + throwable.getMessage().replaceAll("\"", "'");
        }

        if (exceptionBrief.isEmpty()) {
            exceptionBrief = "EXCEPTION 정보가 남지 않았습니다.";
        }

        discordWebhook.addEmbed(new EmbedObject()
                .setTitle("[" + level + " - 문제 간략 내용]")
                .setColor(messageColor)
                .setDescription(exceptionBrief)
                .addField("[" + "Exception Level" + "]",
                        StringEscapeUtils.escapeJson(level),
                        true)
                .addField("[문제 발생 시각]",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        false)
                .addField(
                        "[" + MDCUtil.REQUEST_URI_MDC + "]",
                        StringEscapeUtils.escapeJson(mdcPropertyMap.get(MDCUtil.REQUEST_METHOD_MDC) +"\n"+ mdcPropertyMap.get(MDCUtil.REQUEST_URI_MDC)),
                        false)
                .addField(
                        "[" + MDCUtil.USER_IP_MDC + "]",
                        StringEscapeUtils.escapeJson(mdcPropertyMap.get(MDCUtil.USER_IP_MDC)),
                        false)
                .addField(
                        "[" + MDCUtil.USER_INFO + "]",
                        StringEscapeUtils.escapeJson(mdcPropertyMap.get(MDCUtil.USER_INFO)),
                        false)
                .addField(
                        "[" + MDCUtil.HEADER_MAP_MDC + "]",
                        StringEscapeUtils.escapeJson(mdcPropertyMap.get(MDCUtil.HEADER_MAP_MDC).replaceAll("[\\{\\{\\}]", "")),
                        true)
//                .addField(
//                        "[" + MDCUtil.USER_REQUEST_COOKIES + "]",
//                        StringEscapeUtils.escapeJson(
//                                mdcPropertyMap.get(MDCUtil.USER_REQUEST_COOKIES).replaceAll("[\\{\\{\\}]", "")),
//                        false)
                .addField(
                        "[" + MDCUtil.PARAMETER_MAP_MDC + "]",
                        StringEscapeUtils.escapeJson(
                                mdcPropertyMap.get(MDCUtil.PARAMETER_MAP_MDC).replaceAll("[\\{\\{\\}]", "")),
                        false)
                .addField("[" + MDCUtil.BODY_MDC + "]",
                        StringEscapeUtils.escapeJson(StringUtil.translateEscapes(mdcPropertyMap.get(MDCUtil.BODY_MDC))),
                        false)
        );

        if (throwable != null) {
            String exceptionDetail = ThrowableProxyUtil.asString(throwable);
            String exception = exceptionDetail.substring(0, 4000);
            discordWebhook.addEmbed(
                    new EmbedObject()
                            .setTitle("[Exception 상세 내용]")
                            .setColor(messageColor)
                            .setDescription(StringEscapeUtils.escapeJson(exception))
            );
        }

        try {
            discordWebhook.execute();
        } catch (IOException ioException) {
            throw new BusinessException(BusinessErrorCode.DISCORD_WEBHOOK_ERROR);
        }
    }
}
