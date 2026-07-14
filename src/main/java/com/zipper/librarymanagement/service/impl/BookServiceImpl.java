package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.dto.AddBookDTO;
import com.zipper.librarymanagement.dto.UpdateBookDTO;
import com.zipper.librarymanagement.entity.Book;
import com.zipper.librarymanagement.entity.BorrowRecord;
import com.zipper.librarymanagement.entity.SysCategory;
import com.zipper.librarymanagement.mapper.BookMapper;
import com.zipper.librarymanagement.mapper.BorrowRecordMapper;
import com.zipper.librarymanagement.mapper.SysCategoryMapper;
import com.zipper.librarymanagement.service.BookService;
import com.zipper.librarymanagement.vo.BatchImportResultVO;
import com.zipper.librarymanagement.vo.BookVO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书管理业务实现类。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>图书完整 CRUD 操作（增、删、改、查）</li>
 *   <li>多条件组合搜索，支持分页、关键词、分类、状态筛选及多种排序方式</li>
 *   <li>库存原子操作：借书时扣减库存（{@link #deductStock}）、还书时恢复库存（{@link #restoreStock}）</li>
 *   <li>图书上下架状态管控，下架前校验是否有未归还记录</li>
 *   <li>热门图书查询</li>
 * </ul>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link BookMapper}：图书数据持久化（含物理删除方法）</li>
 *   <li>{@link SysCategoryMapper}：分类信息查询（VO 转换时补充分类名称）</li>
 *   <li>{@link BorrowRecordMapper}：借阅记录查询（状态变更及删除前校验）</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p>凡涉及数据库写操作（新增、更新、删除、库存变更、状态变更）的方法均使用
 * {@code @Transactional} 注解，确保数据一致性。读操作（查询、搜索）不加事务。</p>
 *
 * @see BookService
 * @see Book
 * @see Book.BookStatus
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private SysCategoryMapper sysCategoryMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    /**
     * 多条件分页搜索图书并返回 VO。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构建 MyBatis-Plus 分页对象 {@link Page}</li>
     *   <li>构建动态查询条件：关键词模糊匹配书名/作者/ISBN（OR 关系）</li>
     *   <li>按分类 ID 精确筛选（可选）</li>
     *   <li>按状态精确筛选（可选）</li>
     *   <li>按指定规则排序（stock_asc / stock_desc / 默认按创建时间倒序）</li>
     *   <li>执行分页查询，将 {@link Book} 实体逐一转换为 {@link BookVO}（补充分类名称）</li>
     * </ol>
     *
     * @param page       页码（从 1 开始）
     * @param size       每页条数
     * @param keyword    搜索关键词（模糊匹配书名、作者、ISBN），可为 {@code null}
     * @param categoryId 分类 ID 筛选，可为 {@code null}
     * @param status     图书状态筛选（参见 {@link Book.BookStatus}），可为 {@code null}
     * @param orderBy    排序规则（"stock_asc" / "stock_desc"），可为 {@code null}
     * @return 分页结果，包含 {@link BookVO} 列表
     */
    @Override
    public IPage<BookVO> searchBooks(Integer page, Integer size, String keyword,
                                     Long categoryId, Integer status, String orderBy) {
        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Book::getName, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Book::getStatus, status);
        }
        if ("stock_asc".equals(orderBy)) {
            wrapper.orderByAsc(Book::getStock);
        } else if ("stock_desc".equals(orderBy)) {
            wrapper.orderByDesc(Book::getStock);
        } else {
            wrapper.orderByDesc(Book::getCreateTime);
        }
        IPage<Book> bookPage = page(pageParam, wrapper);
        IPage<BookVO> voPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        List<BookVO> voList = bookPage.getRecords().stream().map(this::toBookVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 根据 ID 获取图书详情。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>通过主键查询图书实体</li>
     *   <li>若不存在则抛出 {@link BusinessException}("图书不存在")</li>
     *   <li>转换为 {@link BookVO}（含分类名称）后返回</li>
     * </ol>
     *
     * @param bookId 图书 ID
     * @return 图书详情 VO，包含分类名称
     * @throws BusinessException 若图书不存在
     */
    @Override
    public BookVO getBookDetail(Long bookId) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        return toBookVO(book);
    }

    /**
     * 获取热门图书列表（Top N）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>筛选状态为"在库"的图书</li>
     *   <li>按创建时间倒序排序</li>
     *   <li>使用 {@code LIMIT} 子句截取前 {@code topN} 本</li>
     *   <li>转换为 {@link BookVO} 列表后返回</li>
     * </ol>
     *
     * <p><b>注意：</b>生产环境应改为按实际借阅次数排序。</p>
     *
     * @param topN 返回数量
     * @return 热门图书 VO 列表
     */
    @Override
    public List<BookVO> getHotBooks(Integer topN) {
        List<Book> books = baseMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, Book.BookStatus.IN_STOCK.getCode())
                        .orderByDesc(Book::getCreateTime)
                        .last("LIMIT " + topN));
        return books.stream().map(this::toBookVO).collect(Collectors.toList());
    }

    /**
     * 新增图书并持久化。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>校验 ISBN 唯一性（已存在则抛出异常）</li>
     *   <li>将 {@link AddBookDTO} 字段映射到 {@link Book} 实体</li>
     *   <li>默认状态设为"在库"（{@link Book.BookStatus#IN_STOCK}）</li>
     *   <li>持久化到数据库</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>该方法标注 {@code @Transactional}，确保原子性写入。</p>
     *
     * @param dto 新增图书数据传输对象
     * @throws BusinessException 若 ISBN 已存在
     */
    @Override
    @Transactional
    public void addBook(AddBookDTO dto) {
        Long count = lambdaQuery().eq(Book::getIsbn, dto.getIsbn()).count();
        if (count > 0) {
            throw new BusinessException("ISBN已存在");
        }
        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setName(dto.getName());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setPrice(dto.getPrice());
        book.setStock(dto.getStock());
        book.setCategoryId(dto.getCategoryId());
        book.setCoverUrl(dto.getCoverUrl());
        book.setIntroduction(dto.getIntroduction());
        book.setStatus(Book.BookStatus.IN_STOCK.getCode());
        save(book);
    }

    /**
     * 更新图书信息。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询图书，不存在则抛出异常</li>
     *   <li>若修改了 ISBN：校验新 ISBN 是否被其他图书占用（排除自身）</li>
     *   <li>将所有 DTO 字段覆盖到实体上</li>
     *   <li>执行更新操作</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>该方法标注 {@code @Transactional}，确保查-改流程的原子性。</p>
     *
     * @param dto 更新图书数据传输对象（必须包含 {@code id}）
     * @throws BusinessException 若图书不存在 或 新 ISBN 已被其他图书使用
     */
    @Override
    @Transactional
    public void updateBook(UpdateBookDTO dto) {
        Book book = getById(dto.getId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (dto.getIsbn() != null && !dto.getIsbn().equals(book.getIsbn())) {
            Long count = lambdaQuery().eq(Book::getIsbn, dto.getIsbn()).ne(Book::getId, dto.getId()).count();
            if (count > 0) {
                throw new BusinessException("ISBN已被其他图书使用");
            }
        }
        book.setIsbn(dto.getIsbn());
        book.setName(dto.getName());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setPrice(dto.getPrice());
        book.setStock(dto.getStock());
        book.setCategoryId(dto.getCategoryId());
        book.setCoverUrl(dto.getCoverUrl());
        book.setIntroduction(dto.getIntroduction());
        updateById(book);
    }

    /**
     * 更新图书状态（上架/下架/标记遗失等）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询图书，不存在则抛出异常</li>
     *   <li>若目标状态为"下架"（{@link Book.BookStatus#OFF_SHELF}）：
     *       校验是否有未归还或逾期的借阅记录（status = 0 或 2）</li>
     *   <li>若存在未归还记录则拒绝下架</li>
     *   <li>更新图书状态并持久化</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>该方法标注 {@code @Transactional}，确保查询-校验-更新的一致性。</p>
     *
     * @param bookId 图书 ID
     * @param status 目标状态枚举值
     * @throws BusinessException 若图书不存在，或下架时仍有未归还记录
     */
    @Override
    @Transactional
    public void updateBookStatus(Long bookId, Book.BookStatus status) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (status == Book.BookStatus.OFF_SHELF) {
            Long borrowingCount = borrowRecordMapper.selectCount(
                    new LambdaQueryWrapper<BorrowRecord>()
                            .eq(BorrowRecord::getBookId, bookId)
                            .in(BorrowRecord::getStatus, 0, 2));
            if (borrowingCount > 0) {
                throw new BusinessException("该图书仍有未归还记录，暂不能下架");
            }
        }
        book.setStatus(status.getCode());
        updateById(book);
    }

    /**
     * 物理删除图书。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询图书，不存在则抛出异常</li>
     *   <li>校验是否有未归还/逾期的借阅记录（status = 0 或 2）</li>
     *   <li>若存在则拒绝删除</li>
     *   <li>执行物理删除</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>该方法标注 {@code @Transactional}。</p>
     *
     * @param bookId 图书 ID
     * @throws BusinessException 若图书不存在 或 存在未归还记录
     */
    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        Long borrowingCount = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getBookId, bookId)
                        .in(BorrowRecord::getStatus, 0, 2));
        if (borrowingCount > 0) {
            throw new BusinessException("该图书有未归还记录，无法删除");
        }
        baseMapper.physicalDeleteById(bookId);
    }

    /**
     * 批量导入图书（通过 Excel 文件）。
     *
     * <p><b>当前状态：未实现。</b>需引入 Apache POI 依赖后方可完成。
     * 实现方案：解析 Excel 工作簿，逐行校验字段后批量 insert。</p>
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果（成功/失败数量统计）
     * @throws BusinessException 始终抛出"批量导入功能尚未实现"
     */
    @Override
    public BatchImportResultVO batchImportBooks(MultipartFile file) {
        BatchImportResultVO result = new BatchImportResultVO();
        List<Book> batch = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String isbn = getCellString(row, 0);
                String name = getCellString(row, 1);
                String author = getCellString(row, 2);
                String publisher = getCellString(row, 3);
                BigDecimal price = getCellDecimal(row, 4);
                Integer stock = getCellInt(row, 5);
                Long categoryId = getCellLong(row, 6);

                if (isbn.isEmpty() || name.isEmpty()) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailMessages().add("第" + (i + 1) + "行：ISBN或书名为空");
                    continue;
                }
                Long cnt = lambdaQuery().eq(Book::getIsbn, isbn).count();
                if (cnt > 0) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailMessages().add("第" + (i + 1) + "行：ISBN " + isbn + " 已存在");
                    continue;
                }
                Book book = new Book();
                book.setIsbn(isbn);
                book.setName(name);
                book.setAuthor(author);
                book.setPublisher(publisher);
                book.setPrice(price != null ? price : BigDecimal.ZERO);
                book.setStock(stock != null ? stock : 0);
                book.setCategoryId(categoryId);
                book.setStatus(Book.BookStatus.IN_STOCK.getCode());
                batch.add(book);
                result.setSuccessCount(result.getSuccessCount() + 1);
            }
            if (!batch.isEmpty()) {
                saveBatch(batch);
            }
        } catch (Exception e) {
            result.setFailCount(result.getFailCount() + 1);
            result.getFailMessages().add("文件解析失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void exportBooks(HttpServletResponse response, String keyword, Long categoryId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Book::getName, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Book::getCreateTime);
        List<Book> books = list(wrapper);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("图书列表");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"ISBN", "书名", "作者", "出版社", "价格", "库存", "分类ID", "状态"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(b.getIsbn());
                row.createCell(1).setCellValue(b.getName());
                row.createCell(2).setCellValue(b.getAuthor());
                row.createCell(3).setCellValue(b.getPublisher());
                row.createCell(4).setCellValue(b.getPrice() != null ? b.getPrice().doubleValue() : 0);
                row.createCell(5).setCellValue(b.getStock());
                row.createCell(6).setCellValue(b.getCategoryId() != null ? b.getCategoryId() : 0);
                row.createCell(7).setCellValue(b.getStatus());
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=books.xlsx");
            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
            }
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        String v = cell.getStringCellValue();
        return v != null ? v.trim() : "";
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try {
            return new BigDecimal(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        try {
            return Integer.parseInt(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Long getCellLong(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        try {
            return Long.parseLong(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 原子扣减库存（图书被借出时调用）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>执行原子 SQL 更新：{@code stock = stock - 1 WHERE stock > 0}</li>
     *   <li>若扣减成功（affected rows > 0，表示库存充足）</li>
     *   <li>重新加载图书，检查库存是否降至 0</li>
     *   <li>若库存为 0，自动将图书状态更新为"全部借出"（{@link Book.BookStatus#ALL_BORROWED}）</li>
     * </ol>
     *
     * <h4>并发安全</h4>
     * <p>使用 {@code stock > 0} 条件做乐观锁，在数据库层面保证不超借。</p>
     *
     * <h4>事务</h4>
     * <p>该方法标注 {@code @Transactional}。</p>
     *
     * @param bookId 图书 ID
     * @return {@code true} 扣减成功（库存 > 0），{@code false} 库存不足
     */
    @Override
    @Transactional
    public boolean deductStock(Long bookId) {
        boolean updated = lambdaUpdate()
                .eq(Book::getId, bookId)
                .gt(Book::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        if (updated) {
            Book book = getById(bookId);
            if (book != null && (book.getStock() == null || book.getStock() <= 0)) {
                lambdaUpdate()
                        .eq(Book::getId, bookId)
                        .set(Book::getStatus, Book.BookStatus.ALL_BORROWED.getCode())
                        .update();
            }
        }
        return updated;
    }

    /**
     * 恢复库存（图书归还时调用）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>执行原子 SQL 更新：{@code stock = stock + 1}</li>
     *   <li>同时将图书状态恢复为"在库"（{@link Book.BookStatus#IN_STOCK}）</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>该方法标注 {@code @Transactional}。</p>
     *
     * @param bookId 图书 ID
     */
    @Override
    @Transactional
    public void restoreStock(Long bookId) {
        lambdaUpdate()
                .eq(Book::getId, bookId)
                .setSql("stock = stock + 1")
                .set(Book::getStatus, Book.BookStatus.IN_STOCK.getCode())
                .update();
    }

    /**
     * 判断指定图书当前是否可借阅。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询图书实体</li>
     *   <li>若实体不存在则返回 {@code false}</li>
     *   <li>调用 {@link Book#isAvailableForBorrow()} 判断库存和状态</li>
     * </ol>
     *
     * @param bookId 图书 ID
     * @return {@code true} 可借阅，{@code false} 不可借阅或不存在
     */
    @Override
    public boolean isAvailableForBorrow(Long bookId) {
        Book book = getById(bookId);
        return book != null && book.isAvailableForBorrow();
    }

    /**
     * 将 {@link Book} 实体转换为 {@link BookVO} 视图对象。
     *
     * <h4>转换逻辑</h4>
     * <ol>
     *   <li>逐字段复制实体属性到 VO</li>
     *   <li>若分类 ID 不为空，通过 {@link SysCategoryMapper} 查询分类名称并填充</li>
     * </ol>
     *
     * <p><b>注意：</b>若列表中每本书都触发一次分类查询，N+1 问题可通过批量预加载优化。</p>
     *
     * @param book 图书实体
     * @return 包含分类名称的图书 VO
     */
    private BookVO toBookVO(Book book) {
        BookVO vo = new BookVO();
        vo.setId(book.getId());
        vo.setIsbn(book.getIsbn());
        vo.setName(book.getName());
        vo.setAuthor(book.getAuthor());
        vo.setPublisher(book.getPublisher());
        vo.setPrice(book.getPrice());
        vo.setStock(book.getStock());
        vo.setCategoryId(book.getCategoryId());
        vo.setCoverUrl(book.getCoverUrl());
        vo.setIntroduction(book.getIntroduction());
        vo.setStatus(book.getStatus());
        vo.setCreateTime(book.getCreateTime());
        if (book.getCategoryId() != null) {
            SysCategory category = sysCategoryMapper.selectById(book.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return vo;
    }
}
