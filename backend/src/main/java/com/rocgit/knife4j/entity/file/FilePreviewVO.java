package com.rocgit.knife4j.entity.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件预览响应（T-12 增强）
 *
 * <ul>
 *   <li>{@code previewable=false} 时前端回退到"下载"按钮</li>
 *   <li>{@code content} 字段在 text/plain 类小文件直接返文本，图片/pdf 返回 null</li>
 *   <li>{@code reason} 给前端友好的兜底文案（needs_convert / too_large / not_supported）</li>
 * </ul>
 */
@Data
@Schema(description = "文件预览响应")
public class FilePreviewVO {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "预览URL（带水印）")
    private String url;

    @Schema(description = "文件类型", example = "pdf")
    private String type;

    @Schema(description = "是否水印", example = "true")
    private Boolean watermark;

    // ===== T-12 增强字段 =====

    @Schema(description = "是否能在线预览", example = "true")
    private Boolean previewable;

    @Schema(description = "预览类型：image / pdf / text / drawio / convert / none",
            example = "pdf")
    private String previewType;

    @Schema(description = "文本类预览的直接内容（仅 text/* 走此字段）")
    private String content;

    @Schema(description = "文本类预览是否被截断", example = "false")
    private Boolean truncated;

    @Schema(description = "总字节数（用于前端展示大小）")
    private Long totalSize;

    @Schema(description = "不可预览时的原因：needs_convert / too_large / not_supported")
    private String reason;
}
