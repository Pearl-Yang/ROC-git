package com.rocgit.knife4j.controller;

import com.rocgit.knife4j.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * T-18：流程图（classpath:static/flowcharts/）元数据接口。
 * 前端按名称 + URL 列表渲染，可放大查看。
 */
@RestController
@RequestMapping("/api/v1/config/flowcharts")
@Tag(name = "12.流程图", description = "系统内置流程图（登录/注册/项目协作等）")
public class FlowchartController {

    /** 中文名映射（按 01~29 顺序） */
    private static final String[] NAMES = new String[] {
            "用户登录流程", "修改个人资料流程", "（占位）", "创建小组流程",
            "（占位）", "（占位）", "（占位）", "（占位）",
            "（占位）", "（占位）", "（占位）", "访客访问分享项目流程",
            "（占位）", "操作日志记录流程", "提交流程（综合）", "（占位）",
            "（占位）", "移除成员流程", "（占位）", "文件预览流程",
            "文件管理流程（删除/移动/收藏）", "版本管理全局流程", "文件检索与筛选流程", "消息通知与订阅流程",
            "协作决策流程（创建/通过/驳回）", "问题提交流程（游客视角）", "1.4 授权流程", "系统总体结构图",
            "组长用例图"
    };

    @Operation(summary = "列出所有内置流程图")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] files = resolver.getResources("classpath:static/flowcharts/flow_*.png");
            // 排序：按文件名
            Arrays.sort(files, Comparator.comparing(Resource::getFilename));
            int idx = 0;
            for (Resource r : files) {
                String fname = r.getFilename();
                if (fname == null) continue;
                String num = fname.replaceAll("flow_(\\d+)\\.png", "$1");
                String name = (idx < NAMES.length) ? NAMES[idx] : ("流程图 " + num);
                Map<String, Object> item = new HashMap<>();
                item.put("no", num);
                item.put("name", name);
                item.put("url", "/flowcharts/" + fname);
                result.add(item);
                idx++;
            }
        } catch (Exception ignore) {}
        return Result.success(result);
    }
}
