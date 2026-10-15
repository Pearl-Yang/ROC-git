package com.rocgit.knife4j.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求。
 * T-01 改造：增加 multipartFile 字段（Controller 反射用），
 * 同时保留旧 byte[] 字段做兼容。
 */
@Data
@Schema(description = "文件上传请求")
public class UploadRequest {

    /** Controller 反射注入的 MultipartFile（不参与 JSON 序列化） */
    @Schema(hidden = true)
    private transient MultipartFile multipartFile;

    @Schema(description = "文件二进制内容（占位 JSON 模式）", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private byte[] file;

    @Schema(description = "上传类型", example = "file",
            allowableValues = {"file", "avatar", "poster"})
    private String type;

    @Schema(description = "所属项目ID", example = "1001")
    private Long projectId;
}