package com.rocgit.knife4j.service;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.dto.preview.PreviewMetaVO;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.mapper.file.FileAuditLogMapper;
import com.rocgit.knife4j.mapper.file.FileMapper;
import com.rocgit.knife4j.service.preview.FileConvertService;
import com.rocgit.knife4j.service.preview.PreviewService;
import com.rocgit.knife4j.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * T-12 PreviewService 单元测试
 *
 * 覆盖：图片直链 / PDF / 文本内联 / Office 触发转码 / 不支持 / 文本截断
 */
class PreviewServiceTest {

    private FileMapper fileMapper;
    private FileAuditLogMapper fileAuditLogMapper;
    private FileStorageService fileStorageService;
    private FileConvertService fileConvertService;
    private PreviewService previewService;

    @BeforeEach
    void setUp() {
        fileMapper = mock(FileMapper.class);
        fileAuditLogMapper = mock(FileAuditLogMapper.class);
        fileStorageService = mock(FileStorageService.class);
        fileConvertService = mock(FileConvertService.class);
        previewService = new PreviewService(fileMapper, fileAuditLogMapper, fileStorageService, fileConvertService);
        ReflectionTestUtils.setField(previewService, "maxTextBytes", 256L);
        ReflectionTestUtils.setField(previewService, "maxImageBytes", 10_485_760L);
        ReflectionTestUtils.setField(previewService, "pdfCacheDir", "/tmp/preview");
        when(fileStorageService.getBaseUrl()).thenReturn("/files");
    }

    @Test
    void preview_imagePng_shouldBePreviewable() {
        FileEntity f = new FileEntity();
        f.setId(1L); f.setName("a.png"); f.setType("png"); f.setSize(1024L); f.setUrl("/files/202610/1_x.png");
        when(fileMapper.selectById(1L)).thenReturn(f);

        PreviewMetaVO vo = previewService.preview(1L);
        assertTrue(vo.getPreviewable());
        assertEquals("image", vo.getPreviewType());
        assertNull(vo.getReason());
    }

    @Test
    void preview_pdf_shouldBePreviewable() {
        FileEntity f = new FileEntity();
        f.setId(2L); f.setName("a.pdf"); f.setType("pdf"); f.setSize(2048L); f.setUrl("/files/202610/2_x.pdf");
        when(fileMapper.selectById(2L)).thenReturn(f);

        PreviewMetaVO vo = previewService.preview(2L);
        assertTrue(vo.getPreviewable());
        assertEquals("pdf", vo.getPreviewType());
    }

    @Test
    void preview_textPlain_shouldInlineContent() throws Exception {
        FileEntity f = new FileEntity();
        f.setId(3L); f.setName("a.txt"); f.setType("txt"); f.setSize(11L); f.setUrl("/files/x/a.txt");
        when(fileMapper.selectById(3L)).thenReturn(f);
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("preview-test-", ".txt");
        java.nio.file.Files.writeString(tmp, "Hello World");
        when(fileStorageService.resolveToDisk("/files/x/a.txt")).thenReturn(tmp);

        PreviewMetaVO vo = previewService.preview(3L);
        assertTrue(vo.getPreviewable());
        assertEquals("text", vo.getPreviewType());
        assertEquals("Hello World", vo.getContent());
        assertFalse(vo.getTruncated());
    }

    @Test
    void preview_textTruncated_shouldSetTruncatedTrue() throws Exception {
        FileEntity f = new FileEntity();
        // 用比 setUp 的 256 字节更大的文件（2048），触发截断
        f.setId(4L); f.setName("big.txt"); f.setType("txt"); f.setSize(2048L); f.setUrl("/files/x/big.txt");
        when(fileMapper.selectById(4L)).thenReturn(f);
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("preview-big-", ".txt");
        byte[] data = new byte[2048];
        for (int i = 0; i < data.length; i++) data[i] = (byte) ('a' + (i % 26));
        java.nio.file.Files.write(tmp, data);
        when(fileStorageService.resolveToDisk("/files/x/big.txt")).thenReturn(tmp);
        // setUp maxTextBytes=256；文件 2048 字节，自然触发截断

        PreviewMetaVO vo = previewService.preview(4L);
        assertEquals("text", vo.getPreviewType());
        assertTrue(vo.getTruncated(), "文件 2048B > maxTextBytes 256，应截断");
        assertEquals(256, vo.getContent().length(), "截断后应为 256 字节");
    }

    @Test
    void preview_officeDocx_shouldTriggerConvert() {
        FileEntity f = new FileEntity();
        f.setId(5L); f.setName("a.docx"); f.setType("docx"); f.setSize(8192L); f.setUrl("/files/x/a.docx");
        when(fileMapper.selectById(5L)).thenReturn(f);
        when(fileConvertService.ensureConverted(anyLong(), any())).thenReturn(null);

        PreviewMetaVO vo = previewService.preview(5L);
        assertFalse(vo.getPreviewable());
        assertEquals("convert", vo.getPreviewType());
        assertEquals("needs_convert", vo.getReason());
        verify(fileConvertService, atLeastOnce()).asyncConvert(anyLong(), any());
    }

    @Test
    void preview_zipNotSupported() {
        FileEntity f = new FileEntity();
        f.setId(6L); f.setName("a.zip"); f.setType("zip"); f.setSize(1024L); f.setUrl("/files/x/a.zip");
        when(fileMapper.selectById(6L)).thenReturn(f);

        PreviewMetaVO vo = previewService.preview(6L);
        assertFalse(vo.getPreviewable());
        assertEquals("none", vo.getPreviewType());
        assertEquals("not_supported", vo.getReason());
    }

    @Test
    void preview_fileNotFound_throws() {
        when(fileMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> previewService.preview(99L));
        assertNotNull(ex);
    }
}
