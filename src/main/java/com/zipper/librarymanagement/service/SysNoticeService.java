package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysNotice;

import java.util.List;

/**
 * 系统公告业务接口
 * <p>管理图书馆公告的发布与展示，支持草稿/发布双状态控制与置顶功能。
 * 学生端只能看到已发布的公告，管理员可以管理全部状态。</p>
 */
public interface SysNoticeService extends IService<SysNotice> {

    /**
     * 分页查询公告列表
     * @param status  公告状态筛选（null=全部，0=草稿，1=已发布）
     * @param keyword 关键词（模糊匹配公告标题）
     */
    IPage<SysNotice> listNotices(Integer page, Integer size,
                                 Integer status, String keyword);

    /**
     * 获取已发布公告列表（学生端首页展示，置顶的排前面）
     */
    List<SysNotice> getPublishedNotices();

    /**
     * 获取置顶的已发布公告
     */
    List<SysNotice> getTopNotices();

    /**
     * 新增公告（默认状态为 DRAFT 草稿）
     */
    void addNotice(SysNotice notice);

    /**
     * 编辑公告
     */
    void updateNotice(SysNotice notice);

    /**
     * 发布公告（DRAFT → PUBLISHED）
     */
    void publishNotice(Long noticeId);

    /**
     * 撤回公告（PUBLISHED → DRAFT）
     */
    void unpublishNotice(Long noticeId);

    /**
     * 删除公告
     */
    void deleteNotice(Long noticeId);
}
