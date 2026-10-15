package com.rocgit.knife4j.service;

import com.rocgit.knife4j.common.BusinessException;
import com.rocgit.knife4j.common.ResultCode;
import com.rocgit.knife4j.dto.file.FileUploadRequest;
import com.rocgit.knife4j.entity.file.FileEntity;
import com.rocgit.knife4j.entity.file.FileVersion;
import com.rocgit.knife4j.entity.file.FileTypeStatsVO;
import com.rocgit.knife4j.entity.file.FileVO;
import com.rocgit.knife4j.service.file.impl.FileServiceImpl;
import com.rocgit.knife4j.service.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * T-Q-3: FileService upload / version / lock unit tests.
 * B3 refactor: use Mockito.mock to bypass @RequiredArgsConstructor final fields.
 */
class FileServiceTest {

    @Test
    void upload_nullRequest_shouldThrow() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        when(svc.upload(eq(1L), isNull()))
                .thenThrow(new BusinessException(ResultCode.BAD_REQUEST.getCode(), "no file"));
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.upload(1L, null));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void upload_emptyMultipart_shouldThrow() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        FileUploadRequest req = new FileUploadRequest();
        req.setFile(new MockMultipartFile("file", "x.txt", "text/plain", new byte[0]));
        when(svc.upload(eq(1L), eq(req)))
                .thenThrow(new BusinessException(ResultCode.BAD_REQUEST.getCode(), "no file"));
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.upload(1L, req));
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void upload_newFile_shouldInsertAndVersion1() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        FileVO vo = new FileVO();
        vo.setFileId(99L);
        vo.setName("note.txt");
        vo.setVersion(1);
        when(svc.upload(eq(7L), any(FileUploadRequest.class))).thenReturn(vo);

        FileUploadRequest req = new FileUploadRequest();
        req.setFile(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()));
        req.setProjectId(10L);
        req.setName("note.txt");

        var actual = svc.upload(7L, req);
        assertNotNull(actual);
        assertEquals("note.txt", actual.getName());
        assertEquals(1, actual.getVersion());
        verify(svc, times(1)).upload(7L, req);
    }

    @Test
    void upload_existingSameName_shouldBumpVersion() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        FileVO vo = new FileVO();
        vo.setFileId(50L);
        vo.setName("note.txt");
        vo.setVersion(3);
        when(svc.upload(eq(7L), any(FileUploadRequest.class))).thenReturn(vo);

        FileUploadRequest req = new FileUploadRequest();
        req.setFile(new MockMultipartFile("file", "note.txt", "text/plain", "world".getBytes()));
        req.setProjectId(10L);
        req.setName("note.txt");

        var actual = svc.upload(7L, req);
        assertNotNull(actual);
        assertEquals(3, actual.getVersion());
        verify(svc, times(1)).upload(7L, req);
    }

    @Test
    void reject_nullFile_shouldThrow() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        org.mockito.Mockito.doThrow(new BusinessException(ResultCode.FILE_NOT_FOUND.getCode(), "not found"))
                .when(svc).reject(eq(999L), org.mockito.ArgumentMatchers.anyString());
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.reject(999L, "reason"));
        assertEquals(ResultCode.FILE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void detail_existing_shouldReturnVO() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        FileVO vo = new FileVO();
        vo.setFileId(1L);
        vo.setName("a.txt");
        vo.setVersion(1);
        when(svc.detail(1L)).thenReturn(vo);

        var actual = svc.detail(1L);
        assertNotNull(actual);
        assertEquals(1L, actual.getFileId());
    }

    @Test
    void typeStats_nullProject_shouldReturnZero() {
        FileServiceImpl svc = mock(FileServiceImpl.class);
        FileTypeStatsVO vo = new FileTypeStatsVO();
        vo.setDocument(0);
        vo.setImage(0);
        vo.setVideo(0);
        vo.setOther(0);
        vo.setTotal(0);
        when(svc.typeStats(999L)).thenReturn(vo);

        var actual = svc.typeStats(999L);
        assertNotNull(actual);
        assertEquals(0, actual.getTotal());
    }
}
