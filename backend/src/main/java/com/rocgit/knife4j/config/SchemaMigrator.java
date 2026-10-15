package com.rocgit.knife4j.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * T-Fix-1：自动执行 database 目录下的 03-update.sql / 04-update.sql ...
 *  - 启动后扫描 classpath:database/*-update.sql（按文件名排序）
 *  - 维护 schema_migration 表，记录已执行的脚本
 *  - 单条语句失败时只跳过该条（兼容 ALTER 重复执行）
 */
@Component
public class SchemaMigrator {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private final JdbcTemplate jdbc;

    @Value("${rocgit.schema.migrate:true}")
    private boolean enabled;

    public SchemaMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        if (!enabled) {
            log.info("[SchemaMigrator] 已关闭（rocgit.schema.migrate=false）");
            return;
        }
        try {
            ensureTable();
            Set<String> applied = loadApplied();
            List<Resource> resources = scanScripts();
            int totalExecuted = 0, totalSkipped = 0, totalFailed = 0;
            for (Resource r : resources) {
                String name = r.getFilename();
                if (name == null) continue;
                if (applied.contains(name)) {
                    log.debug("[SchemaMigrator] 已执行过，跳过 {}", name);
                    totalSkipped++;
                    continue;
                }
                List<String> stmts = readStatements(r);
                if (stmts.isEmpty()) continue;
                int ok = 0, fail = 0;
                for (String s : stmts) {
                    try {
                        jdbc.execute(s);
                        ok++;
                    } catch (Exception e) {
                        // 重复列 / 重复索引 / 等已应用的情况视为"已存在"，不阻断
                        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                        if (msg.contains("duplicate") || msg.contains("exists")
                                || msg.contains("already") || msg.contains("重复")) {
                            ok++;
                        } else {
                            log.warn("[SchemaMigrator] {} 语句失败: {} -> {}", name, s, e.getMessage());
                            fail++;
                        }
                    }
                }
                if (ok > 0) {
                    markApplied(name);
                    totalExecuted += ok;
                    log.info("[SchemaMigrator] {} 执行 {} 条成功，{} 条失败", name, ok, fail);
                }
                totalFailed += fail;
            }
            if (totalExecuted > 0 || totalFailed > 0) {
                log.info("[SchemaMigrator] 完成：执行 {} 条，跳过 {} 个文件，失败 {} 条", totalExecuted, totalSkipped, totalFailed);
            }
        } catch (Exception e) {
            log.error("[SchemaMigrator] 迁移失败：{}", e.getMessage());
        }
    }

    private void ensureTable() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS `schema_migration` (\n" +
                "  `name` VARCHAR(128) NOT NULL,\n" +
                "  `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "  PRIMARY KEY (`name`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private Set<String> loadApplied() {
        try {
            List<String> list = jdbc.queryForList("SELECT name FROM schema_migration", String.class);
            return new HashSet<>(list);
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private void markApplied(String name) {
        jdbc.update("INSERT IGNORE INTO schema_migration(name) VALUES(?)", name);
    }

    private List<Resource> scanScripts() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 同时扫描两个目录：src/main/resources/db/（运行时）和 backend/database/（开发顶层）
        List<Resource> list = new ArrayList<>();
        try {
            Collections.addAll(list, resolver.getResources("classpath:database/*-update.sql"));
        } catch (Exception ignore) {}
        try {
            Collections.addAll(list, resolver.getResources("classpath:db/*-update.sql"));
        } catch (Exception ignore) {}
        // 按文件名排序（去重）
        Set<String> seen = new HashSet<>();
        List<Resource> uniq = new ArrayList<>();
        for (Resource r : list) {
            String n = r.getFilename();
            if (n != null && seen.add(n)) uniq.add(r);
        }
        uniq.sort(Comparator.comparing(Resource::getFilename));
        return uniq;
    }

    private List<String> readStatements(Resource r) throws Exception {
        List<String> stmts = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                // 跳过注释和空行
                if (t.isEmpty() || t.startsWith("--")) continue;
                buf.append(line).append('\n');
                if (t.endsWith(";")) {
                    String s = buf.toString().trim();
                    // 去尾分号
                    if (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();
                    if (!s.isEmpty()) stmts.add(s);
                    buf.setLength(0);
                }
            }
            // 兜底：无尾分号
            String tail = buf.toString().trim();
            if (!tail.isEmpty()) stmts.add(tail);
        }
        return stmts;
    }
}
