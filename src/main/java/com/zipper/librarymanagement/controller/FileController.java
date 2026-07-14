package com.zipper.librarymanagement.controller;

import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * <p>处理用户头像和图书封面的上传、替换与删除。
 * 文件按类型分目录存储：{@code uploads/avatars/} 和 {@code uploads/covers/}。</p>
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    private Path uploadDir;

    private final HttpServletRequest request;

    @PostConstruct
    public void init() {
        Path p = Paths.get(uploadPath);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir"), uploadPath);
        }
        this.uploadDir = p.normalize().toAbsolutePath();
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(uploadDir.resolve("avatars"));
            Files.createDirectories(uploadDir.resolve("covers"));
            log.info("上传目录: {}", uploadDir);
        } catch (IOException e) {
            log.error("无法创建上传目录: {}", uploadDir, e);
        }
    }

    /**
     * 根据文件名（最后一段）在 uploads 目录下查找并删除文件。
     * 先查找顶级目录，再查找 avatars/ 和 covers/ 子目录。
     */
    private void deleteFileByName(String filename) {
        try {
            if (Files.deleteIfExists(uploadDir.resolve(filename))) return;
            if (Files.deleteIfExists(uploadDir.resolve("avatars").resolve(filename))) return;
            Files.deleteIfExists(uploadDir.resolve("covers").resolve(filename));
        } catch (Exception e) {
            log.warn("删除文件失败: {}", filename, e);
        }
    }

    @Operation(summary = "上传文件", description = "上传文件到服务器。通过 subdir 参数指定子目录（avatars/covers），传入 oldUrl 时自动删除旧文件。")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @Parameter(description = "待上传的文件") @RequestParam MultipartFile file,
            @Parameter(description = "旧文件URL（可选，上传新文件替换时删除旧文件）") @RequestParam(required = false) String oldUrl,
            @Parameter(description = "存储子目录（avatars | covers），默认存放在 uploads 根目录") @RequestParam(required = false) String subdir) {
        RoleUtil.requireAdmin(request);
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (oldUrl != null && !oldUrl.isEmpty()) {
            String filename = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
            deleteFileByName(filename);
        }
        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String savedName = UUID.randomUUID().toString() + extension;
            Path targetDir = (subdir != null && !subdir.isEmpty())
                    ? uploadDir.resolve(subdir) : uploadDir;
            Files.createDirectories(targetDir);
            Path filePath = targetDir.resolve(savedName);
            file.transferTo(filePath.toFile());
            String url = (subdir != null && !subdir.isEmpty())
                    ? "/uploads/" + subdir + "/" + savedName
                    : "/uploads/" + savedName;
            log.info("文件上传成功: {} -> {}", originalName, url);
            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除文件", description = "根据文件URL删除服务器上的对应文件。")
    @DeleteMapping("/delete")
    public Result<Void> delete(@Parameter(description = "文件URL（如 /uploads/avatars/xxx.png）") @RequestParam String url) {
        RoleUtil.requireAdmin(request);
        if (url == null || url.isEmpty()) {
            return Result.error("URL不能为空");
        }
        String filename = url.substring(url.lastIndexOf("/") + 1);
        deleteFileByName(filename);
        log.info("文件已删除: {}", url);
        return Result.success();
    }
}
