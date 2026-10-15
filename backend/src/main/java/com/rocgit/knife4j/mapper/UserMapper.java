package com.rocgit.knife4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rocgit.knife4j.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** T-06：统计用户上传的文件数 */
    @Select("SELECT COUNT(*) FROM file WHERE uploader_id = #{userId} AND deleted = 0")
    Long countFilesUploaded(@org.apache.ibatis.annotations.Param("userId") Long userId);

    /** T-06：统计用户上传且通过审核的文件数 */
    @Select("SELECT COUNT(*) FROM file WHERE uploader_id = #{userId} AND status = 'approved' AND deleted = 0")
    Long countFilesApproved(@org.apache.ibatis.annotations.Param("userId") Long userId);

    /** T-06：统计用户完成的任务数 */
    @Select("SELECT COUNT(*) FROM task WHERE assignee_id = #{userId} AND status = 'completed' AND deleted = 0")
    Long countCompletedTasks(@org.apache.ibatis.annotations.Param("userId") Long userId);

    /** T-06：统计用户发起的研讨数 */
    @Select("SELECT COUNT(*) FROM discussion WHERE creator_id = #{userId} AND deleted = 0")
    Long countDiscussions(@org.apache.ibatis.annotations.Param("userId") Long userId);
}