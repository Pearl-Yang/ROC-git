package com.rocgit.knife4j.entity.version;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 版本对比结果
 */
@Data
@Schema(description = "版本对比")
public class VersionCompareVO {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "起始版本")
    private String fromVersion;

    @Schema(description = "目标版本")
    private String toVersion;

    @Schema(description = "差异对比URL")
    private String diffUrl;

    @Schema(description = "新增行数")
    private Integer additions;

    @Schema(description = "删除行数")
    private Integer deletions;

    @Schema(description = "差异片段列表")
    private List<DiffHunk> hunks;

    // ========== T-03c 新增字段（与原 hunks 兼容） ==========

    @Schema(description = "差异行集合（type/add/del/same + n1/n2/content）")
    private List<Map<String, Object>> diffLines;

    @Schema(description = "起始版本行数")
    private Integer fromLineCount;

    @Schema(description = "目标版本行数")
    private Integer toLineCount;

    @Schema(description = "新增数（与 additions 同义，保留别名）")
    private Long added;

    @Schema(description = "删除数（与 deletions 同义，保留别名）")
    private Long removed;

    @Schema(description = "错误信息：非文本类时填")
    private String error;

    @Schema(description = "起始版本元信息")
    private Map<String, Object> fromMeta;

    @Schema(description = "目标版本元信息")
    private Map<String, Object> toMeta;

    @Data
    @Schema(description = "差异片段")
    public static class DiffHunk {
        private Integer startLine;
        private String content;
        private String type;
    }
}