package com.rocgit.knife4j.service.version.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rocgit.knife4j.common.UserContext;
import com.rocgit.knife4j.dto.version.VersionRollbackRequest;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.entity.file.FileVersion;
import com.rocgit.knife4j.entity.version.FileVersionVO;
import com.rocgit.knife4j.entity.version.VersionCompareVO;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.mapper.file.FileVersionMapper;
import com.rocgit.knife4j.service.version.FileVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FileVersionServiceImpl implements FileVersionService {

    @Autowired private FileVersionMapper fileVersionMapper;
    @Autowired private FileMapper fileMapper;

    @Override
    public List<FileVersionVO> list(Long fileId) {
        List<FileVersion> list = fileVersionMapper.selectList(
                new QueryWrapper<FileVersion>().eq("file_id", fileId).eq("deleted", 0).orderByDesc("created_at"));
        List<FileVersionVO> result = new ArrayList<>();
        for (FileVersion v : list) {
            FileVersionVO vo = new FileVersionVO();
            vo.setVersionId(v.getId());
            vo.setFileId(v.getFileId());
            vo.setVersionNumber(v.getVersionNumber());
            vo.setUrl(v.getUrl());
            vo.setRemark(v.getRemark());
            vo.setOperatorId(v.getOperatorId());
            vo.setCreatedAt(v.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    @Override
    public VersionCompareVO compare(Long fileId, String from, String to) {
        VersionCompareVO vo = new VersionCompareVO();
        vo.setFileId(fileId);
        vo.setFromVersion(from);
        vo.setToVersion(to);

        FileVersion fv = fileVersionMapper.selectOne(
                new QueryWrapper<FileVersion>().eq("file_id", fileId).eq("version_number", from).eq("deleted", 0).last("LIMIT 1"));
        FileVersion tv = fileVersionMapper.selectOne(
                new QueryWrapper<FileVersion>().eq("file_id", fileId).eq("version_number", to).eq("deleted", 0).last("LIMIT 1"));
        if (fv == null || tv == null) {
            vo.setError("版本不存在");
            return vo;
        }

        // T-03c：尝试按文本行做 LCS diff；若 url 不支持文本读取则只返元信息
        try {
            List<String> fromLines = readUrlAsText(fv.getUrl());
            List<String> toLines = readUrlAsText(tv.getUrl());
            List<Map<String, Object>> diff = lineLevelDiff(fromLines, toLines);
            vo.setDiffLines(diff);
            vo.setFromLineCount(fromLines.size());
            vo.setToLineCount(toLines.size());
            vo.setAdded(diff.stream().filter(d -> "add".equals(d.get("type"))).count());
            vo.setRemoved(diff.stream().filter(d -> "del".equals(d.get("type"))).count());
        } catch (Exception e) {
            // 非文本类（docx/png/...）返元信息对比
            vo.setError("非文本类文件，仅做元数据对比: " + e.getMessage());
            vo.setFromMeta(Map.of("url", fv.getUrl(), "createdAt", String.valueOf(fv.getCreatedAt())));
            vo.setToMeta(Map.of("url", tv.getUrl(), "createdAt", String.valueOf(tv.getCreatedAt())));
        }
        return vo;
    }

    @Override
    @Transactional
    public void rollback(Long fileId, VersionRollbackRequest request) {
        if (request.getTargetVersion() == null) return;
        FileVersion target = fileVersionMapper.selectOne(
                new QueryWrapper<FileVersion>()
                        .eq("file_id", fileId)
                        .eq("version_number", request.getTargetVersion())
                        .eq("deleted", 0)
                        .last("LIMIT 1"));
        if (target == null) return;
        FileEntity f = fileMapper.selectById(fileId);
        if (f == null) return;

        // 1. 找出当前最新版本号，递增 +1 作为"恢复版本"号
        List<FileVersion> all = fileVersionMapper.selectList(
                new QueryWrapper<FileVersion>().eq("file_id", fileId).eq("deleted", 0).orderByDesc("created_at"));
        String nextVer = bumpVersion(all.isEmpty() ? "1.0" : all.get(0).getVersionNumber());

        // 2. 写回 file.url（保持"最新有效文件 url"语义）
        f.setUrl(target.getUrl());
        f.setVersion(f.getVersion() == null ? 1 : f.getVersion() + 1);
        fileMapper.updateById(f);

        // 3. 关键：新增一条 file_version 记录（F-051 历史可追溯）
        FileVersion rv = new FileVersion();
        rv.setFileId(fileId);
        rv.setVersionNumber(nextVer);
        rv.setUrl(target.getUrl());
        rv.setRemark("回滚自 " + request.getTargetVersion() + (request.getRemark() == null ? "" : "：" + request.getRemark()));
        try {
            rv.setOperatorId(UserContext.currentUserId() == null ? 0L : UserContext.currentUserId());
        } catch (Exception e) {
            rv.setOperatorId(0L);
        }
        fileVersionMapper.insert(rv);
    }

    /** 把 "1.0" / "2.3" 这种版本号末位 +1，返回 "1.1"/"2.4" */
    private String bumpVersion(String base) {
        if (base == null || base.isEmpty()) return "1.1";
        int dot = base.lastIndexOf('.');
        if (dot < 0) {
            try { return (Integer.parseInt(base) + 1) + ".0"; } catch (Exception e) { return base + ".1"; }
        }
        try {
            int minor = Integer.parseInt(base.substring(dot + 1));
            return base.substring(0, dot + 1) + (minor + 1);
        } catch (Exception e) {
            return base + ".1";
        }
    }

    /** 读取远端 url 内容为按行文本。仅在文本类文件有效。 */
    private List<String> readUrlAsText(String url) throws Exception {
        if (url == null || url.isEmpty()) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    /** 简易行级 diff：逐行比对，输出 {type:add/del/same, n1, n2, content}。非 LCS，O(n*m)，文件 ≤ 200 行够用。 */
    private List<Map<String, Object>> lineLevelDiff(List<String> a, List<String> b) {
        int n = a.size(), m = b.size();
        // dp[i][j] = a[:i] 与 b[:j] 的 LCS 长度
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        // 回溯生成 diff
        List<Map<String, Object>> diff = new ArrayList<>();
        int i = n, j = m;
        LinkedList<Map<String, Object>> stack = new LinkedList<>();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && a.get(i - 1).equals(b.get(j - 1))) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("type", "same"); e.put("n1", i); e.put("n2", j); e.put("content", a.get(i - 1));
                stack.addFirst(e); i--; j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("type", "add"); e.put("n1", 0); e.put("n2", j); e.put("content", b.get(j - 1));
                stack.addFirst(e); j--;
            } else if (i > 0) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("type", "del"); e.put("n1", i); e.put("n2", 0); e.put("content", a.get(i - 1));
                stack.addFirst(e); i--;
            }
        }
        diff.addAll(stack);
        return diff;
    }
}