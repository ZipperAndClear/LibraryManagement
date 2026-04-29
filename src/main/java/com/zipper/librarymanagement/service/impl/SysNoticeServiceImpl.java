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
 * 系统公告业务实现类
 * <p>管理公告的完整生命周期：草稿 → 发布 → 撤回。
 * 支持置顶功能（置顶的公告在列表中排在前面）。
 * 学生端查询时只返回已发布的公告。</p>
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    @Override
    public IPage<SysNotice> listNotices(Integer page, Integer size,
                                        Integer status, String keyword) {
        Page<SysNotice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        // 按状态筛选
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        // 按标题关键词模糊搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysNotice::getTitle, keyword);
        }
        // 排序规则：置顶优先 → 按时间倒序
        wrapper.orderByDesc(SysNotice::getIsTop).orderByDesc(SysNotice::getCreateTime);
        return page(pageParam, wrapper);
    }

    @Override
    public List<SysNotice> getPublishedNotices() {
        return lambdaQuery()
                .eq(SysNotice::getStatus, SysNotice.NoticeStatus.PUBLISHED.getCode())
                .orderByDesc(SysNotice::getIsTop)
                .orderByDesc(SysNotice::getCreateTime)
                .list();
    }

    @Override
    public List<SysNotice> getTopNotices() {
        return lambdaQuery()
                .eq(SysNotice::getStatus, SysNotice.NoticeStatus.PUBLISHED.getCode())
                .eq(SysNotice::getIsTop, 1)
                .orderByDesc(SysNotice::getCreateTime)
                .list();
    }

    @Override
    @Transactional
    public void addNotice(SysNotice notice) {
        // 新公告默认为草稿状态、不置顶
        if (notice.getStatus() == null) {
            notice.setStatus(SysNotice.NoticeStatus.DRAFT.getCode());
        }
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        save(notice);
    }

    @Override
    @Transactional
    public void updateNotice(SysNotice notice) {
        SysNotice exist = getById(notice.getId());
        if (exist == null) {
            throw new BusinessException("公告不存在");
        }
        updateById(notice);
    }

    @Override
    @Transactional
    public void publishNotice(Long noticeId) {
        SysNotice notice = getById(noticeId);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        // 状态流转：DRAFT → PUBLISHED
        notice.setStatus(SysNotice.NoticeStatus.PUBLISHED.getCode());
        updateById(notice);
    }

    @Override
    @Transactional
    public void unpublishNotice(Long noticeId) {
        SysNotice notice = getById(noticeId);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        // 状态流转：PUBLISHED → DRAFT
        notice.setStatus(SysNotice.NoticeStatus.DRAFT.getCode());
        updateById(notice);
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        removeById(noticeId);
    }
}
