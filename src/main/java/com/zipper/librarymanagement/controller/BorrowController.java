package com.zipper.librarymanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.service.BorrowRecordService;
import com.zipper.librarymanagement.vo.BorrowRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "借阅管理")
@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowRecordService borrowRecordService;
    private final HttpServletRequest request;

    @Operation(summary = "借阅图书", description = "学生借阅图书的完整流程：校验用户状态、借阅限额、逾期记录、罚款情况、图书库存，通过后扣减库存并生成借阅记录。此操作涉及多表事务。")
    @Parameters({
            @Parameter(name = "userId", description = "借阅人（用户）ID"),
            @Parameter(name = "bookId", description = "借阅图书ID")
    })
    @PostMapping("/borrow")
    public Result<Void> borrow(@RequestParam Long userId,
                                @RequestParam Long bookId) {
        borrowRecordService.borrowBook(userId, bookId);
        return Result.success();
    }

    @Operation(summary = "归还图书", description = "归还图书流程：恢复图书库存，判定是否逾期，逾期则自动计算罚金并生成罚款单。操作涉及多表事务。")
    @Parameters({
            @Parameter(name = "borrowRecordId", description = "借阅记录ID"),
            @Parameter(name = "userId", description = "归还人（用户）ID")
    })
    @PostMapping("/return")
    public Result<Void> returnBook(@RequestParam Long borrowRecordId,
                                    @RequestParam Long userId) {
        RoleUtil.requireAdmin(request);
        borrowRecordService.returnBook(borrowRecordId, userId);
        return Result.success();
    }

    @Operation(summary = "续借图书", description = "在借阅截止前延长应还时间。校验：仅借阅中状态可续借、逾期不可续借、续借次数不可超限。")
    @Parameters({
            @Parameter(name = "borrowRecordId", description = "借阅记录ID"),
            @Parameter(name = "userId", description = "续借人（用户）ID")
    })
    @PostMapping("/renew")
    public Result<Void> renew(@RequestParam Long borrowRecordId,
                               @RequestParam Long userId) {
        borrowRecordService.renewBook(borrowRecordId, userId);
        return Result.success();
    }

    @Operation(summary = "标记图书丢失", description = "管理员操作。将借阅记录标记为图书丢失，同时更新图书状态为遗失，并按图书定价生成赔偿罚款单。")
    @PostMapping("/mark-lost")
    public Result<Void> markLost(@Parameter(description = "借阅记录ID") @RequestParam Long borrowRecordId) {
        RoleUtil.requireAdmin(request);
        borrowRecordService.markBookLost(borrowRecordId);
        return Result.success();
    }

    @Operation(summary = "分页查询借阅记录", description = "查询借阅记录列表，支持按用户、状态、图书名称关键词筛选。返回结果包含用户名、书名、罚款金额等关联信息。")
    @Parameters({
            @Parameter(name = "page", description = "页码"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "userId", description = "用户ID筛选（学生端只能查自己的）"),
            @Parameter(name = "status", description = "借阅状态筛选：0-借阅中 1-正常归还 2-逾期未还 3-逾期已还 4-图书遗失"),
            @Parameter(name = "keyword", description = "图书名称关键词搜索")
    })
    @GetMapping("/list")
    public Result<IPage<BorrowRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword) {
        IPage<BorrowRecordVO> result = borrowRecordService.listRecords(page, size, userId, status, keyword);
        return Result.success(result);
    }

    @Operation(summary = "逾期未还列表", description = "查询所有已超过应还时间但尚未归还的借阅记录列表，供管理员催还使用。")
    @GetMapping("/overdue")
    public Result<List<BorrowRecordVO>> overdue() {
        RoleUtil.requireAdmin(request);
        List<BorrowRecordVO> records = borrowRecordService.getOverdueList();
        return Result.success(records);
    }
}
