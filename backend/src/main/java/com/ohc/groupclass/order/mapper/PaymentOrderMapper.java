package com.ohc.groupclass.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohc.groupclass.order.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("SELECT * FROM payment_order "
            + "WHERE status = 'PAYING' AND expire_time <= #{now} "
            + "ORDER BY expire_time LIMIT #{limit}")
    List<PaymentOrder> selectExpired(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
