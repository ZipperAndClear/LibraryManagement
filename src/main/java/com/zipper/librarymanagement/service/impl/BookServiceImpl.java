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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书管理业务实现类
 * <p>实现了图书的完整 CRUD、多条件搜索、库存原子操作、
 * 上下架状态管控等功能。库存扣减使用原子 SQL 防止超借。</p>
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private SysCategoryMapper sysCategoryMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    public IPage<BookVO> searchBooks(Integer page, Integer size, String keyword,
                                     Long categoryId, Integer status, String orderBy) {
        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        // 关键词模糊匹配：书名 OR 作者 OR ISBN
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Book::getName, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        // 分类筛选
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }
        // 状态筛选
        if (status != null) {
            wrapper.eq(Book::getStatus, status);
        }
        // 排序
        if ("stock_asc".equals(orderBy)) {
            wrapper.orderByAsc(Book::getStock);
        } else if ("stock_desc".equals(orderBy)) {
            wrapper.orderByDesc(Book::getStock);
        } else {
            wrapper.orderByDesc(Book::getCreateTime);
        }
        // 查询并转换 VO
        IPage<Book> bookPage = page(pageParam, wrapper);
        IPage<BookVO> voPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        List<BookVO> voList = bookPage.getRecords().stream().map(this::toBookVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public BookVO getBookDetail(Long bookId) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        return toBookVO(book);
    }

    @Override
    public List<BookVO> getHotBooks(Integer topN) {
        // 按创建时间倒序取前 N 本（生产环境应改为按借阅次数排序）
        List<Book> books = baseMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, Book.BookStatus.IN_STOCK.getCode())
                        .orderByDesc(Book::getCreateTime)
                        .last("LIMIT " + topN));
        return books.stream().map(this::toBookVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addBook(AddBookDTO dto) {
        // 校验 ISBN 唯一性
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

    @Override
    @Transactional
    public void updateBook(UpdateBookDTO dto) {
        Book book = getById(dto.getId());
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        // 若修改了 ISBN，校验新 ISBN 是否被其他图书使用
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

    @Override
    @Transactional
    public void updateBookStatus(Long bookId, Book.BookStatus status) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        // 下架前需确认无未归还的借阅记录
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

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        // 校验是否有未归还的借阅记录
        Long borrowingCount = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getBookId, bookId)
                        .in(BorrowRecord::getStatus, 0, 2));
        if (borrowingCount > 0) {
            throw new BusinessException("该图书有未归还记录，无法删除");
        }
        removeById(bookId);
    }

    @Override
    public BatchImportResultVO batchImportBooks(MultipartFile file) {
        // 占位实现：Excel 解析逻辑待实现
        BatchImportResultVO result = new BatchImportResultVO();
        result.setSuccessCount(0);
        return result;
    }

    @Override
    public void exportBooks(HttpServletResponse response, String keyword, Long categoryId) {
        // 占位实现：Excel 导出逻辑待实现
    }

    @Override
    @Transactional
    public boolean deductStock(Long bookId) {
        // 原子化扣减库存：stock = stock - 1，确保 stock > 0
        boolean updated = lambdaUpdate()
                .eq(Book::getId, bookId)
                .gt(Book::getStock, 0)
                .setSql("stock = stock - 1")
                .update();
        if (updated) {
            // 扣减后检查库存是否为 0，若是则自动更新状态为"全部借出"
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

    @Override
    @Transactional
    public void restoreStock(Long bookId) {
        lambdaUpdate()
                .eq(Book::getId, bookId)
                .setSql("stock = stock + 1")
                .set(Book::getStatus, Book.BookStatus.IN_STOCK.getCode())
                .update();
    }

    @Override
    public boolean isAvailableForBorrow(Long bookId) {
        Book book = getById(bookId);
        return book != null && book.isAvailableForBorrow();
    }

    /**
     * 将 Book Entity 转换为 BookVO（补充分类名称）
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
        // 补充分类名称（跨表查询）
        if (book.getCategoryId() != null) {
            SysCategory category = sysCategoryMapper.selectById(book.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return vo;
    }
}
