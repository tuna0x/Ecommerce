package com.tuna.ecommerce.controller;

import java.io.IOException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.Banner;
import com.tuna.ecommerce.domain.request.banner.ReqCreateBannerDTO;
import com.tuna.ecommerce.domain.request.banner.ReqUpdateBannerDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.service.BannerService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;
import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class BannerController {
    private final BannerService bannerService;

    @PostMapping("/banners")
    @APIMessage("Create new banner")
    public ResponseEntity<Banner> createBanner(@Valid @ModelAttribute ReqCreateBannerDTO banner) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.bannerService.handleCreate(banner, banner.getFile()));
    }

    @PutMapping("/banners")
    @APIMessage("Update banner")
    public ResponseEntity<Banner> updateBanner(@Valid @ModelAttribute ReqUpdateBannerDTO banner) throws IdInvalidException, IOException {
        if (banner == null || banner.getId() == null) {
            throw new IdInvalidException("Banner ID cannot be null for update operation");
        }
        Banner updatedBanner = this.bannerService.handleUpdate(banner, banner.getFile());
        if (updatedBanner == null) {
            throw new IdInvalidException("Banner not found");
        }
        return ResponseEntity.ok().body(updatedBanner);
    }

    @PatchMapping("/banners/{id}/active")
    @APIMessage("Toggle banner active status")
    public ResponseEntity<Banner> toggleActive(@PathVariable("id") long id, @RequestParam("isActive") boolean isActive) throws IdInvalidException {
        Banner updated = this.bannerService.handleToggleActive(id, isActive);
        if (updated == null) {
            throw new IdInvalidException("Banner not found");
        }
        return ResponseEntity.ok().body(updated);
    }

    @DeleteMapping("/banners/{id}")
    @APIMessage("Delete banner by id")
    public ResponseEntity<Void> deleteBanner(@PathVariable("id") long id) throws IdInvalidException, IOException {
        Banner cur = this.bannerService.handleGetById(id);
        if (cur == null) {
            throw new IdInvalidException("Banner not found with id: " + id);
        }
        this.bannerService.handleDelete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/banners/{id}")
    @APIMessage("Get banner by id")
    public ResponseEntity<Banner> getBannerById(@PathVariable Long id) throws IdInvalidException {
        Banner banner = this.bannerService.handleGetById(id);
        if (banner == null) {
            throw new IdInvalidException("Banner not found with id: " + id);
        }
        return ResponseEntity.ok(banner);
    }

    @GetMapping("/banners")
    @APIMessage("Get all banners with filter and pagination")
    public ResponseEntity<ResultPaginationDTO> getAllBanners(@Filter Specification<Banner> spec, Pageable page) {
        return ResponseEntity.ok().body(this.bannerService.handleGetAll(spec, page));
    }
}
