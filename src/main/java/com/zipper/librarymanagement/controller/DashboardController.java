package com.zipper.librarymanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.entity.BorrowRecord;
import com.zipper.librarymanagement.service.BookService;
import com.zipper.librarymanagement.service.BorrowRecordService;
import com.zipper.librarymanagement.vo.BookVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "数据大盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BookService bookService;
    private final BorrowRecordService borrowRecordService;
    private final HttpServletRequest request;

    @Operation(summary = "获取统计数据", description = "返回管理员首页数据大盘：馆藏总数、今日借出数量、当前借阅中数量、逾期未还数量等统计数据。")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        RoleUtil.requireAdmin(request);
        Map<String, Object> data = new HashMap<>();
        long totalBooks = bookService.count();
        data.put("totalBooks", totalBooks);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayBorrows = borrowRecordService.count(
                new LambdaQueryWrapper<BorrowRecord>()
                        .between(BorrowRecord::getBorrowTime, todayStart, todayStart.plusDays(1)));
        data.put("todayBorrows", todayBorrows);
        long currentBorrowing = borrowRecordService.count(
                new LambdaQueryWrapper<BorrowRecord>()
                        .in(BorrowRecord::getStatus, 0, 2));
        data.put("currentBorrowing", currentBorrowing);
        long overdue = borrowRecordService.count(
                new LambdaQueryWrapper<BorrowRecord>()
                        .lt(BorrowRecord::getExpectReturnTime, LocalDateTime.now())
                        .in(BorrowRecord::getStatus, 0, 2));
        data.put("overdue", overdue);
        return Result.success(data);
    }

    @Operation(summary = "热门借阅榜单", description = "获取借阅次数最多的图书排行列表，默认取前10本，用于管理员首页展示。")
    @GetMapping("/hot-books")
    public Result<List<BookVO>> hotBooks() {
        RoleUtil.requireAdmin(request);
        List<BookVO> books = bookService.getHotBooks(10);
        return Result.success(books);
    }
}
