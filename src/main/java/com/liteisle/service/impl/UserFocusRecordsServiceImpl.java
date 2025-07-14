package com.liteisle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liteisle.common.domain.UserFocusRecords;
import com.liteisle.common.domain.UserIslands;
import com.liteisle.common.domain.response.FocusCalendarResp;
import com.liteisle.common.domain.response.FocusStatsPageResp;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.module.chain.manager.FocusRewardChainManager;
import com.liteisle.service.UserFocusRecordsService;
import com.liteisle.mapper.UserFocusRecordsMapper;
import com.liteisle.service.UserIslandsService;
import com.liteisle.util.SignUtil;
import com.liteisle.util.UserContextHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

/**
 * @author 11965
 * @description 针对表【user_focus_records(用户单次专注会话记录表)】的数据库操作Service实现
 * @createDate 2025-07-10 20:09:48
 */
@Slf4j
@Service
public class UserFocusRecordsServiceImpl extends ServiceImpl<UserFocusRecordsMapper, UserFocusRecords>
        implements UserFocusRecordsService {

    @Resource
    private FocusRewardChainManager focusRewardChainManager;
    @Resource
    private UserIslandsService userIslandsService;
    @Resource
    private SignUtil signUtil;
    @Resource
    private UserFocusRecordsMapper userFocusRecordsMapper;

    @Override
    public String createFocusRecord(Integer min) {
        if (min <= 0) {
            throw new LiteisleException("参数错误");
        }
        //记录用户专注记录
        Long userId = UserContextHolder.getUserId();
        UserFocusRecords records = new UserFocusRecords();
        records.setUserId(userId);
        records.setFocusMinutes(min);
        boolean save = this.save(records);
        if (!save) {
            throw new LiteisleException("创建专注记录失败");
        }

        // 签到逻辑
        if (min >= 15) {
            // 直接执行签到，doSign会返回一个布尔值，true代表今天是第一次签到成功
            boolean isFirstSignToday = signUtil.doSign(userId);
            if (isFirstSignToday) {
                log.info("用户 {} 完成今日首次签到。", userId);
                // 可以在这里触发首次签到的奖励
            } else {
                // 如果 isFirstSignToday 为 false，说明用户今天已经签到过了
                log.info("用户 {} 今日已签到，累计专注时长。", userId);
            }
        }

        //执行责任链判断是否会获取岛屿
        String awardedIslandUrl = focusRewardChainManager.executeChain(min);

        if (awardedIslandUrl != null) {
            log.info("用户{}获得奖励的岛屿：{}",userId, awardedIslandUrl);
            UserIslands newUserIsland = new UserIslands();
            newUserIsland.setUserId(userId);
            newUserIsland.setIslandUrl(awardedIslandUrl);
            userIslandsService.save(newUserIsland);
            return awardedIslandUrl;
        }
        return null;
    }

    @Override
    public Long getFocusTotalCount() {
        Long userId = UserContextHolder.getUserId();
        return this.count(new QueryWrapper<UserFocusRecords>().eq("user_id", userId));
    }

    @Override
    public FocusCalendarResp getFocusCalendar(Integer year, Integer month) {
        Long userId = UserContextHolder.getUserId();

        YearMonth yearMonth = YearMonth.of(year, month);
        FocusCalendarResp resp = new FocusCalendarResp();
        resp.setYearMonth(year + "-" + month);
        resp.setTotalCheckInCount((int) signUtil.getSignCount(userId,yearMonth));
        resp.setTotalFocusMinutes(userFocusRecordsMapper.getTotalFocusMinutesInMonth(year, month, userId));
        List<Integer> signedDays = signUtil.getSignDaysForMonth(userId,yearMonth);
        resp.setCheckInDays(signedDays);
        return resp;
    }

    @Override
    public IPage<FocusStatsPageResp.FocusRecord> getFocusRecords(Page<FocusStatsPageResp.FocusRecord> page) {
        Long currentUserId = UserContextHolder.getUserId();
        // 直接调用Mapper方法，MP插件会自动完成所有分页工作
        return userFocusRecordsMapper.selectFocusRecordsPage(page, currentUserId);
    }


}




