package com.rocgit.knife4j.entity.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 贡献度统计（按成员聚合）
 */
@Data
@Schema(description = "贡献度统计")
public class ContributionStatsVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "总贡献分")
    private Integer score;

    @Schema(description = "文件上传次数")
    private Integer fileUploads;

    @Schema(description = "文件审核次数")
    private Integer fileAudits;

    @Schema(description = "文件下载次数")
    private Integer fileDownloads;

    @Schema(description = "完成任务数")
    private Integer tasksCompleted;

    @Schema(description = "逾期任务数（扣分项）")
    private Integer overdueTasks;

    @Schema(description = "排名（1-based）")
    private Integer rank;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActive;
}
