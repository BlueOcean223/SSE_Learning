package com.xuecheng.orders.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.orders.model.po.XcOrdersGoods;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface XcOrdersGoodsMapper extends BaseMapper<XcOrdersGoods> {

    // 批量插入
    int insertBatch(List<XcOrdersGoods> xcOrdersGoods);
}
