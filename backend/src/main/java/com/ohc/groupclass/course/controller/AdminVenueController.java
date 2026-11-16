package com.ohc.groupclass.course.controller;

import com.ohc.groupclass.common.Result;
import com.ohc.groupclass.course.dto.VenueCreateRequest;
import com.ohc.groupclass.course.dto.VenueUpdateRequest;
import com.ohc.groupclass.course.dto.VenueVO;
import com.ohc.groupclass.course.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/venues")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVenueController {

    private final VenueService venueService;

    @GetMapping
    public Result<List<VenueVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(venueService.list(status));
    }

    @GetMapping("/{id}")
    public Result<VenueVO> get(@PathVariable Long id) {
        return Result.ok(venueService.getById(id));
    }

    @PostMapping
    public Result<VenueVO> create(@Valid @RequestBody VenueCreateRequest request) {
        return Result.ok(venueService.create(request));
    }

    @PutMapping("/{id}")
    public Result<VenueVO> update(@PathVariable Long id,
                                  @Valid @RequestBody VenueUpdateRequest request) {
        return Result.ok(venueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        venueService.disable(id);
        return Result.ok();
    }
}
