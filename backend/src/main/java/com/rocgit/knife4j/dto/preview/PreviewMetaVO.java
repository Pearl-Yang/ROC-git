package com.rocgit.knife4j.dto.preview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件预览分发请求
 *
 * <p>前端在打开预览抽屉时调一次 {@code GET /api/v1/preview/{fileId}}，由后端按
 * Content-Type 分发到 image / pdf / text / drawio / convert-pending / not-supported 之一。
 */
@Data
@Schema(description = "预览元信息（GET /api/v1/preview/{fileId} 响应）")
public class PreviewMetaVO {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "扩展名（小写）", example = "pdf")
    private String ext;

    @Schema(description = "MIME 类型", example = "application/pdf")
    private String contentType;

    @Schema(description = "文件总字节数")
    private Long totalSize;

    @Schema(description = "图片/PDF 等可直链的 URL（H5 走 /files 反代；云端切 OSS 后为签名 URL）")
    private String url;

    @Schema(description = "是否能在线预览", example = "true")
    private Boolean previewable;

    @Schema(description = "预览类型：image / pdf / text / drawio / convert / none")
    private String previewType;

    @Schema(description = "文本类预览直接返内容（仅 text/plain|md|csv|json|xml|log）")
    private String content;

    @Schema(description = "文本类预览是否被截断")
    private Boolean truncated;

    @Schema(description = "docx/xlsx/pptx 是否已转码完成", example = "false")
    private Boolean converted;

    @Schema(description = "已转码文件的预览 URL（PDF 格式）")
    private String convertedUrl;

    @Schema(description = "不可预览原因：needs_convert / too_large / not_supported / missing")
    private String reason;
}
