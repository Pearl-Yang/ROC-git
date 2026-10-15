package com.rocgit.knife4j.service.sms.impl;

import com.rocgit.knife4j.service.sms.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志兜底短信通道（T-25，默认激活）
 *
 * <p>行为：
 * <ol>
 *   <li>控制台 INFO 输出验证码</li>
 *   <li>追加到 logs/sms-YYYY-MM-DD.log（容器内是 /app/logs/）</li>
 * </ol>
 *
 * <p>这样本地演示和答辩都能在终端直接看到 6 位码，<b>不需要任何 SMS 凭证</b>。
 *
 * <p>激活条件：{@code sms.provider=log} 或未配置（matchIfMissing=true）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsProvider implements SmsProvider {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String channel() {
        return "log";
    }

    @Override
    public boolean sendCode(String phone, String code, String purpose) {
        String line = String.format("[%s] phone=%s code=%s purpose=%s channel=log",
                LocalDateTime.now().format(TS_FMT), phone, code, purpose);

        // 1) 控制台
        log.info("[SMS] " + line);

        // 2) 文件（容器内 /app/logs/、开发环境 backend/logs/）
        try {
            Path dir = resolveLogDir();
            Files.createDirectories(dir);
            Path file = dir.resolve("sms-" + LocalDate.now() + ".log");
            Files.writeString(file, line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("[SMS] 写日志文件失败（但控制台已输出）: {}", e.getMessage());
        }
        return true;
    }

    private Path resolveLogDir() {
        // 优先级：SystemProperty sms.log.dir > 工作目录下 logs/
        String custom = System.getProperty("sms.log.dir");
        if (custom != null && !custom.isEmpty()) {
            return Paths.get(custom);
        }
        return Paths.get(System.getProperty("user.dir"), "logs");
    }
}
