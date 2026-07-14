package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.entity.SysNotice;
import com.zipper.librarymanagement.mapper.SysNoticeMapper;
import com.zipper.librarymanagement.service.SysNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统公告业务实现类。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>公告完整生命周期管理：草稿 → 发布 → 撤回</li>
 *   <li>公告列表查询：支持分页、状态筛选、关键词搜索</li>
 *   <li>置顶功能：置顶公告在列表排序中优先显示</li>
 *   <li>面向学生端的已发布公告查询（仅返回已发布状态）</li>
 * </ul>
 *
 * <h3>公告状态流转</h3>
 * <pre>
 *   DRAFT(0) ──publish──→ PUBLISHED(1)
 *   PUBLISHED(1) ──unpublish──→ DRAFT(0)
 * </pre>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link SysNoticeMapper}：公告数据持久化</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p>所有写操作（新增、更新、发布、撤回、删除）均使用 {@code @Transactional} 注解。
 * 读操作不加事务。</p>
 *
 * @see SysNoticeService
 * @see SysNotice
 * @see SysNotice.NoticeStatus
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    /**
     * 分页查询公告列表（管理端使用，可查所有状态）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构建分页查询条件：按状态筛选（可选）、按标题关键词模糊搜索（可选）</li>
     *   <li><b>排序规则</b>：置顶优先（isTop 倒序）→ 创建时间倒序</li>
     *   <li>执行分页查询并返回</li>
     * </ol>
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param status  状态筛选（参见 {@link SysNotice.NoticeStatus}），可为 {@code null}
     * @param keyword 标题关键词，可为 {@code null}
     * @return 分页结果
     */
    @Override
    public IPage<SysNotice> listNotices(Integer page, Integer size,
                                        Integer status, String keyword) {
        Page<SysNotice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysNotice::getTitle, keyword);
        }
        wrapper.orderByDesc(SysNotice::getIsTop).orderByDesc(SysNotice::getCreateTime);
        return page(pageParam, wrapper);
    }

    /**
     * 获取所有已发布公告（学生端使用）。
     *
     * <p>筛选条件：状态为"已发布"（{@link SysNotice.NoticeStatus#PUBLISHED}）。
     * 排序：置顶优先 → 创建时间倒序。</p>
     *
     * @return 已发布公告列表
     */
    @Override
    public List<SysNotice> getPublishedNotices() {
        return lambdaQuery()
                .eq(SysNotice::getStatus, SysNotice.NoticeStatus.PUBLISHED.getCode())
                .orderByDesc(SysNotice::getIsTop)
                .orderByDesc(SysNotice::getCreateTime)
                .list();
    }

    /**
     * 获取已发布且置顶的公告列表。
     *
     * <p>筛选条件：状态为"已发布" 且 isTop = 1。
     * 排序：创建时间倒序。</p>
     *
     * @return 置顶公告列表
     */
    @Override
    public List<SysNotice> getTopNotices() {
        return lambdaQuery()
                .eq(SysNotice::getStatus, SysNotice.NoticeStatus.PUBLISHED.getCode())
                .eq(SysNotice::getIsTop, 1)
                .orderByDesc(SysNotice::getCreateTime)
                .list();
    }

    /**
     * 新增公告。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>若未指定状态，默认设为"草稿"（{@link SysNotice.NoticeStatus#DRAFT}）</li>
     *   <li>若未指定是否置顶，默认不置顶（isTop = 0）</li>
     *   <li>持久化到数据库</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param notice 公告实体（需至少包含 title 和 content，可选 status 和 isTop）
     */
    @Override
    @Transactional
    public void addNotice(SysNotice notice) {
        if (notice.getStatus() == null) {
            notice.setStatus(SysNotice.NoticeStatus.DRAFT.getCode());
        }
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        save(notice);
    }

    /**
     * 更新公告内容。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询公告是否存在，不存在则抛出异常</li>
     *   <li>执行全量更新</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param notice 公告实体（必须包含 id）
     * @throws BusinessException 若公告不存在
     */
    @Override
    @Transactional
    public void updateNotice(SysNotice notice) {
        SysNotice exist = getById(notice.getId());
        if (exist == null) {
            throw new BusinessException("公告不存在");
        }
        updateById(notice);
    }

    /**
     * 发布公告：将草稿状态变更为已发布。
     *
     * <h4>状态流转</h4>
     * <p>{@code DRAFT(0) → PUBLISHED(1)}</p>
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询公告，不存在则抛出异常</li>
     *   <li>将状态更新为"已发布"并持久化</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param noticeId 公告 ID
     * @throws BusinessException 若公告不存在
     */
    @Override
    @Transactional
    public void publishNotice(Long noticeId) {
        SysNotice notice = getById(noticeId);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        notice.setStatus(SysNotice.NoticeStatus.PUBLISHED.getCode());
        updateById(notice);
    }

    /**
     * 撤回公告：将已发布状态回退为草稿。
     *
     * <h4>状态流转</h4>
     * <p>{@code PUBLISHED(1) → DRAFT(0)}</p>
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询公告，不存在则抛出异常</li>
     *   <li>将状态更新为"草稿"并持久化</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param noticeId 公告 ID
     * @throws BusinessException 若公告不存在
     */
    @Override
    @Transactional
    public void unpublishNotice(Long noticeId) {
        SysNotice notice = getById(noticeId);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        notice.setStatus(SysNotice.NoticeStatus.DRAFT.getCode());
        updateById(notice);
    }

    /**
     * 删除公告（物理删除）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>按 ID 执行物理删除</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param noticeId 公告 ID
     */
    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        removeById(noticeId);
    }
}
