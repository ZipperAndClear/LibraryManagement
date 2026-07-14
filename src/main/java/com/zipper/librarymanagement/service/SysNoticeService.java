package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysNotice;

import java.util.List;

/**
 * 系统公告业务接口
 * <p>管理图书馆公告的发布与展示，支持草稿/发布双状态控制与置顶功能。
 * 学生端只能看到已发布的公告，管理员可以管理全部状态。</p>
 *
 * <p>公告状态定义：
 * <ul>
 *   <li>{@code 0} — 草稿（DRAFT），尚未发布，仅管理员可见</li>
 *   <li>{@code 1} — 已发布（PUBLISHED），所有用户可见</li>
 * </ul>
 * </p>
 *
 * @author zipper
 * @since 1.0
 */
public interface SysNoticeService extends IService<SysNotice> {

    /**
     * 分页查询公告列表（管理员端）
     * <p>支持按公告状态筛选及标题关键词模糊搜索，返回分页结果。</p>
     *
     * @param page    页码，从 1 开始
     * @param size    每页记录条数
     * @param status  公告状态筛选（{@code null} 表示全部，{@code 0} 表示草稿，{@code 1} 表示已发布）
     * @param keyword 关键词（模糊匹配公告标题，{@code null} 或空字符串表示不筛选）
     * @return 包含公告实体列表的分页对象，按创建时间倒序排列
     */
    IPage<SysNotice> listNotices(Integer page, Integer size,
                                 Integer status, String keyword);

    /**
     * 获取已发布公告列表（学生端首页展示）
     * <p>仅返回状态为已发布（{@code status=1}）的公告，
     * 置顶公告排在前面，再按发布时间倒序排列。</p>
     *
     * @return 已发布公告列表，无数据时返回空列表
     */
    List<SysNotice> getPublishedNotices();

    /**
     * 获取置顶的已发布公告
     * <p>仅返回同时满足“已发布”且“置顶”条件的公告，
     * 通常用于首页轮播或顶部横幅展示。</p>
     *
     * @return 置顶的已发布公告列表，无数据时返回空列表
     */
    List<SysNotice> getTopNotices();

    /**
     * 新增公告
     * <p>新创建的公告默认状态为草稿（{@code status=0}），
     * 需要后续调用 {@link #publishNotice(Long)} 进行发布。</p>
     *
     * @param notice 公告实体，需包含标题、内容等必要字段
     * @throws com.zipper.librarymanagement.exception.BusinessException 若标题为空或必填字段缺失
     */
    void addNotice(SysNotice notice);

    /**
     * 编辑已有公告
     * <p>可修改公告的标题、内容、置顶状态等字段。
     * 公告 ID 必须存在，否则操作失败。</p>
     *
     * @param notice 公告实体，必须包含有效的公告 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若公告不存在
     */
    void updateNotice(SysNotice notice);

    /**
     * 发布公告
     * <p>将指定公告的状态从草稿（{@code 0}）变更为已发布（{@code 1}）。
     * 若公告已是已发布状态，则此操作无副作用。</p>
     *
     * @param noticeId 待发布的公告 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若公告不存在
     */
    void publishNotice(Long noticeId);

    /**
     * 撤回公告
     * <p>将指定公告的状态从已发布（{@code 1}）回退为草稿（{@code 0}）。
     * 撤回后学生端将不再显示该公告。</p>
     *
     * @param noticeId 待撤回的公告 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若公告不存在
     */
    void unpublishNotice(Long noticeId);

    /**
     * 删除公告（物理删除）
     * <p>直接从数据库中移除该公告记录，不可恢复。删除前不校验公告状态，
     * 草稿和已发布的公告均可被删除。</p>
     *
     * @param noticeId 待删除的公告 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若公告不存在
     */
    void deleteNotice(Long noticeId);
}
