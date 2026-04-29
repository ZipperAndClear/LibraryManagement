package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.dto.AddBookDTO;
import com.zipper.librarymanagement.dto.UpdateBookDTO;
import com.zipper.librarymanagement.entity.Book;
import com.zipper.librarymanagement.vo.BatchImportResultVO;
import com.zipper.librarymanagement.vo.BookVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图书管理业务接口
 * <p>涵盖图书的增删改查、多条件搜索、库存管理、热门排行榜以及
 * Excel 批量导入导出功能。库存扣减和恢复使用了原子化 SQL 操作。</p>
 */
public interface BookService extends IService<Book> {

    /**
     * 多条件分页搜索图书
     * @param keyword    关键词（模糊匹配 书名/作者/ISBN）
     * @param categoryId 分类 ID 筛选（null=全部）
     * @param status     图书状态筛选（null=全部）
     * @param orderBy    排序方式（stock_asc/stock_desc/time_desc）
     * @return 分页结果，每项包含分类名称
     */
    IPage<BookVO> searchBooks(Integer page, Integer size, String keyword,
                              Long categoryId, Integer status, String orderBy);

    /**
     * 获取图书详情
     */
    BookVO getBookDetail(Long bookId);

    /**
     * 获取热门借阅排行榜
     * @param topN 取前 N 本
     */
    List<BookVO> getHotBooks(Integer topN);

    /**
     * 新增图书
     * <p>校验 ISBN 唯一性</p>
     */
    void addBook(AddBookDTO dto);

    /**
     * 编辑图书信息
     * <p>修改 ISBN 时需校验新 ISBN 未被其他图书使用</p>
     */
    void updateBook(UpdateBookDTO dto);

    /**
     * 更新图书上下架状态
     * <p>下架前校验该图书是否有未归还的借阅记录，有则禁止下架</p>
     */
    void updateBookStatus(Long bookId, Book.BookStatus status);

    /**
     * 删除图书（逻辑删除）
     * <p>校验该图书是否有未归还的借阅记录，有则禁止删除</p>
     */
    void deleteBook(Long bookId);

    /**
     * 批量导入图书（Excel 文件解析）
     */
    BatchImportResultVO batchImportBooks(MultipartFile file);

    /**
     * 导出图书列表为 Excel
     */
    void exportBooks(HttpServletResponse response, String keyword, Long categoryId);

    /**
     * 扣减图书库存（借书时调用）
     * <p>使用原子化 SQL：stock = stock - 1，同时校验 stock > 0，
     * 若返回 false 表示库存不足。当库存扣减到 0 时自动更新状态为 ALL_BORROWED。</p>
     */
    boolean deductStock(Long bookId);

    /**
     * 恢复图书库存（还书或取消借阅时调用）
     * <p>stock + 1，并将状态恢复为 IN_STOCK</p>
     */
    void restoreStock(Long bookId);

    /**
     * 检查图书是否可借
     * <p>条件：status = IN_STOCK 且 stock > 0</p>
     */
    boolean isAvailableForBorrow(Long bookId);
}
