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
 * 图书管理服务接口
 * <p>定义图书从入库到淘汰的完整生命周期操作。包括：</p>
 * <ul>
 *   <li>图书馆藏管理：新增、编辑、删除</li>
 *   <li>图书检索：多条件搜索、详情查询、热门排行</li>
 *   <li>库存操作：借出扣减、归还恢复（原子化操作，防止并发超借）</li>
 *   <li>状态流转：在库 → 全部借出 → 下架 → 遗失</li>
 *   <li>批量操作：Excel 导入/导出</li>
 * </ul>
 */
public interface BookService extends IService<Book> {

    /**
     * 多条件分页搜索图书
     *
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @param keyword    搜索关键词（模糊匹配书名、作者、ISBN）
     * @param categoryId 分类 ID（null 表示全部）
     * @param status     图书状态筛选（null 表示全部）
     * @param orderBy    排序方式（stock_asc / stock_desc / 默认按创建时间倒序）
     * @return 包含分类名称的 BookVO 分页结果
     */
    IPage<BookVO> searchBooks(Integer page, Integer size, String keyword,
                              Long categoryId, Integer status, String orderBy);

    /**
     * 获取单个图书的完整信息
     *
     * @param bookId 图书 ID
     * @return 包含分类名称的图书详情
     * @throws BusinessException 图书不存在时抛出
     */
    BookVO getBookDetail(Long bookId);

    /**
     * 获取热门图书排行榜
     * <p>当前实现为按创建时间倒序取在库图书；生产环境应改为按借阅次数统计</p>
     *
     * @param topN 取前 N 本
     * @return 图书列表（按热度降序）
     */
    List<BookVO> getHotBooks(Integer topN);

    /**
     * 新增一条图书记录
     * <p>新书默认状态为"在库"。ISBN 必须全局唯一。</p>
     *
     * @param dto 包含 ISBN、书名、作者、出版社、价格、库存、分类、封面、简介
     * @throws BusinessException ISBN 已存在时抛出
     */
    void addBook(AddBookDTO dto);

    /**
     * 编辑已有图书信息
     * <p>若修改了 ISBN，需校验新 ISBN 未被其他图书占用</p>
     *
     * @param dto 包含图书 ID 及需修改的字段
     * @throws BusinessException 图书不存在或 ISBN 冲突时抛出
     */
    void updateBook(UpdateBookDTO dto);

    /**
     * 更新图书状态（在库/全部借出/下架/遗失）
     * <p>下架前会校验是否有未归还的借阅记录</p>
     *
     * @param bookId 图书 ID
     * @param status 目标状态枚举值
     * @throws BusinessException 图书不存在或下架时有未归还记录时抛出
     */
    void updateBookStatus(Long bookId, Book.BookStatus status);

    /**
     * 删除图书记录（物理删除）
     * <p>有未归还借阅记录时禁止删除</p>
     *
     * @param bookId 图书 ID
     * @throws BusinessException 图书不存在或有未归还记录时抛出
     */
    void deleteBook(Long bookId);

    /**
     * 从 Excel 批量导入图书
     *
     * @param file Excel 文件（.xlsx 格式）
     * @return 导入结果：成功数量 + 失败数量 + 失败原因明细
     */
    BatchImportResultVO batchImportBooks(MultipartFile file);

    /**
     * 导出图书列表为 Excel 并直接写入 HTTP 响应流
     *
     * @param response   HTTP 响应（用于输出文件流）
     * @param keyword    导出时的关键词筛选条件
     * @param categoryId 导出时的分类筛选条件
     */
    void exportBooks(HttpServletResponse response, String keyword, Long categoryId);

    /**
     * 原子化扣减库存（stock = stock - 1 WHERE stock > 0）
     * <p>使用乐观锁防止并发超借。如果扣减后库存归零，自动将图书状态更新为"全部借出"</p>
     *
     * @param bookId 图书 ID
     * @return true=扣减成功，false=库存不足
     */
    boolean deductStock(Long bookId);

    /**
     * 归还图书时恢复库存（stock = stock + 1）
     * <p>同时将图书状态置回"在库"</p>
     */
    void restoreStock(Long bookId);

    /**
     * 检查图书是否当前可借阅
     * <p>判断条件：状态为"在库"且库存大于 0</p>
     *
     * @param bookId 图书 ID
     * @return true=可借阅
     */
    boolean isAvailableForBorrow(Long bookId);
}
