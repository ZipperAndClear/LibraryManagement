package com.zipper.librarymanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.dto.AddBookDTO;
import com.zipper.librarymanagement.dto.UpdateBookDTO;
import com.zipper.librarymanagement.entity.Book;
import com.zipper.librarymanagement.service.BookService;
import com.zipper.librarymanagement.vo.BatchImportResultVO;
import com.zipper.librarymanagement.vo.BookVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "图书管理")
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final HttpServletRequest request;

    @Operation(summary = "分页搜索图书", description = "支持多条件组合搜索：关键词（书名/作者/ISBN模糊匹配）、分类筛选、状态筛选、排序。返回结果包含分类名称。")
    @Parameters({
            @Parameter(name = "page", description = "页码"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "keyword", description = "关键词（模糊匹配书名、作者、ISBN）"),
            @Parameter(name = "categoryId", description = "分类ID筛选"),
            @Parameter(name = "status", description = "图书状态筛选：1-在库 2-全部借出 3-下架 4-遗失"),
            @Parameter(name = "orderBy", description = "排序方式：stock_asc-库存升序 stock_desc-库存降序 time_desc-时间倒序")
    })
    @GetMapping("/search")
    public Result<IPage<BookVO>> search(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String orderBy) {
        IPage<BookVO> result = bookService.searchBooks(page, size, keyword, categoryId, status, orderBy);
        return Result.success(result);
    }

    @Operation(summary = "图书详情", description = "根据图书ID获取完整信息，包含分类名称。")
    @GetMapping("/detail/{id}")
    public Result<BookVO> detail(@Parameter(description = "图书ID") @PathVariable Long id) {
        BookVO book = bookService.getBookDetail(id);
        return Result.success(book);
    }

    @Operation(summary = "热门图书排行榜", description = "获取借阅次数最多的前N本图书列表，默认取前10本。")
    @GetMapping("/hot")
    public Result<List<BookVO>> hot(@Parameter(description = "取前N本（默认10）") @RequestParam(defaultValue = "10") Integer topN) {
        List<BookVO> books = bookService.getHotBooks(topN);
        return Result.success(books);
    }

    @Operation(summary = "新增图书", description = "录入新图书，ISBN必须全局唯一。新书默认状态为在库。")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody AddBookDTO dto) {
        RoleUtil.requireAdmin(request);
        bookService.addBook(dto);
        return Result.success();
    }

    @Operation(summary = "编辑图书", description = "修改图书信息，若修改ISBN需校验新ISBN未被其他图书占用。")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody UpdateBookDTO dto) {
        RoleUtil.requireAdmin(request);
        bookService.updateBook(dto);
        return Result.success();
    }

    @Operation(summary = "更新图书状态", description = "修改图书的上下架/遗失等状态。下架前会校验是否有未归还的借阅记录，有则禁止下架。")
    @Parameters({
            @Parameter(name = "bookId", description = "图书ID"),
            @Parameter(name = "status", description = "目标状态：1-在库 2-全部借出 3-下架 4-遗失")
    })
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long bookId,
                                     @RequestParam Integer status) {
        RoleUtil.requireAdmin(request);
        bookService.updateBookStatus(bookId, Book.BookStatus.fromCode(status));
        return Result.success();
    }

    @Operation(summary = "删除图书", description = "逻辑删除图书。如有未归还的借阅记录则禁止删除。")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "图书ID") @PathVariable Long id) {
        RoleUtil.requireSuperAdmin(request);
        bookService.deleteBook(id);
        return Result.success();
    }

    @Operation(summary = "批量导入图书", description = "通过Excel文件批量导入图书数据，返回导入成功/失败的数量及失败原因明细。")
    @PostMapping("/import")
    public Result<BatchImportResultVO> importBooks(@Parameter(description = "Excel文件（.xlsx格式）") @RequestParam MultipartFile file) {
        BatchImportResultVO result = bookService.batchImportBooks(file);
        return Result.success(result);
    }

    @Operation(summary = "导出图书列表", description = "按搜索条件导出图书列表为Excel文件并直接下载，条件不传则导出全部。")
    @Parameters({
            @Parameter(name = "keyword", description = "导出条件：关键词"),
            @Parameter(name = "categoryId", description = "导出条件：分类ID")
    })
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       HttpServletResponse response) {
        RoleUtil.requireAdmin(request);
        bookService.exportBooks(response, keyword, categoryId);
    }
}
