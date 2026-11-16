package com.ohc.groupclass.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.dto.VenueCreateRequest;
import com.ohc.groupclass.course.dto.VenueUpdateRequest;
import com.ohc.groupclass.course.dto.VenueVO;
import com.ohc.groupclass.course.entity.Venue;
import com.ohc.groupclass.course.mapper.VenueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueMapper venueMapper;

    public List<VenueVO> list(Integer status) {
        LambdaQueryWrapper<Venue> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(Venue::getStatus, status);
        }
        query.orderByAsc(Venue::getId);
        return venueMapper.selectList(query).stream().map(this::toVO).toList();
    }

    public VenueVO getById(Long id) {
        return toVO(requireVenue(id));
    }

    public Venue requireVenue(Long id) {
        Venue entity = venueMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "场地不存在");
        }
        return entity;
    }

    @Transactional
    public VenueVO create(VenueCreateRequest request) {
        if (countByName(request.getName(), null) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "场地名称已存在");
        }
        Venue entity = new Venue();
        entity.setName(request.getName());
        entity.setAddress(request.getAddress());
        entity.setCapacity(request.getCapacity());
        entity.setStatus(1);
        try {
            venueMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "场地名称已存在");
        }
        return toVO(entity);
    }

    @Transactional
    public VenueVO update(Long id, VenueUpdateRequest request) {
        Venue entity = requireVenue(id);
        if (countByName(request.getName(), id) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "场地名称已存在");
        }
        entity.setName(request.getName());
        entity.setAddress(request.getAddress());
        entity.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            entity.setStatus(validateStatus(request.getStatus()));
        }
        try {
            venueMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CONFLICT, "场地名称已存在");
        }
        return toVO(entity);
    }

    @Transactional
    public void disable(Long id) {
        Venue entity = requireVenue(id);
        entity.setStatus(0);
        venueMapper.updateById(entity);
    }

    private int countByName(String name, Long excludeId) {
        LambdaQueryWrapper<Venue> query = Wrappers.<Venue>lambdaQuery().eq(Venue::getName, name);
        if (excludeId != null) {
            query.ne(Venue::getId, excludeId);
        }
        return Math.toIntExact(venueMapper.selectCount(query));
    }

    private int validateStatus(Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        return status;
    }

    private VenueVO toVO(Venue entity) {
        return new VenueVO(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getCapacity(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
