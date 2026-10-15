package com.rocgit.knife4j.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求（multipart/form-data，T-01）
 *
 * 必填：file（真实文件）；选填：projectId、stageId、remark。
 * name/size/type 由后端从 MultipartFile#getOriginalFilename / getSize / getContentType 推导。
 */
@Data
@Schema(description = "文件上传请求（multipart）")
public class FileUploadRequest {

    @Schema(description = "上传文件本身（必填）", hidden = true)
    private MultipartFile file;

    @Schema(description = "文件名（可选，未填则取 file.originalFilename）")
    private String name;

    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    @Schema(description = "目标阶段ID", example = "2")
    private Long stageId;

    @Schema(description = "文件直传 URL（兼容旧字段，可空）")
    private String url;

    @Schema(description = "文件大小（字节，可选）")
    private Long size;

    @Schema(description = "文件扩展名", example = "docx")
    private String type;

    @Schema(description = "上传说明 / 版本备注")
    private String remark;
}
