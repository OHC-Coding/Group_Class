package com.ohc.groupclass.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ohc.groupclass.common.BusinessException;
import com.ohc.groupclass.common.ResultCode;
import com.ohc.groupclass.course.dto.CourseTemplateCreateRequest;
import com.ohc.groupclass.course.dto.CourseTemplateUpdateRequest;
import com.ohc.groupclass.course.dto.CourseTemplateVO;
import com.ohc.groupclass.course.entity.CourseTemplate;
import com.ohc.groupclass.course.mapper.CourseTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseTemplateService {

    private final CourseTemplateMapper courseTemplateMapper;

    public List<CourseTemplateVO> list(Integer status) {
        LambdaQueryWrapper<CourseTemplate> query = Wrappers.lambdaQuery();
        if (status != null) {
            query.eq(CourseTemplate::getStatus, status);
        }
        query.orderByDesc(CourseTemplate::getId);
        return courseTemplateMapper.selectList(query).stream().map(this::toVO).toList();
    }

    public CourseTemplateVO getById(Long id) {
        return toVO(requireTemplate(id));
    }

    public CourseTemplate requireTemplate(Long id) {
        CourseTemplate entity = courseTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程模板不存在");
        }
        return entity;
    }

    @Transactional
    public CourseTemplateVO create(CourseTemplateCreateRequest request) {
        CourseTemplate entity = new CourseTemplate();
        entity.setName(request.getName());
        entity.setSportType(request.getSportType());
        entity.setDefaultSessions(request.getDefaultSessions());
        entity.setDefaultPrice(request.getDefaultPrice());
        entity.setDescription(request.getDescription());
        entity.setStatus(1);
        courseTemplateMapper.insert(entity);
        return toVO(entity);
    }

    @Transactional
    public CourseTemplateVO update(Long id, CourseTemplateUpdateRequest request) {
        CourseTemplate entity = requireTemplate(id);
        entity.setName(request.getName());
        entity.setSportType(request.getSportType());
        entity.setDefaultSessions(request.getDefaultSessions());
        entity.setDefaultPrice(request.getDefaultPrice());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(validateStatus(request.getStatus()));
        }
        courseTemplateMapper.updateById(entity);
        return toVO(entity);
    }

    @Transactional
    public void disable(Long id) {
        CourseTemplate entity = requireTemplate(id);
        entity.setStatus(0);
        courseTemplateMapper.updateById(entity);
    }

    private int validateStatus(Integer status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        return status;
    }

    private CourseTemplateVO toVO(CourseTemplate entity) {
        return new CourseTemplateVO(
                entity.getId(),
                entity.getName(),
                entity.getSportType(),
                entity.getDefaultSessions(),
                entity.getDefaultPrice(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}
