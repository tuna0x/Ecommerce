package com.tuna.ecommerce.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tuna.ecommerce.domain.Banner;
import com.tuna.ecommerce.domain.request.banner.ReqCreateBannerDTO;
import com.tuna.ecommerce.domain.request.banner.ReqUpdateBannerDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.banner.ResBannerDTO;
import com.tuna.ecommerce.repository.BannerRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class BannerService {
    private final BannerRepository bannerRepository;
    private final CloudinaryService cloudinaryService;

    @CacheEvict(value = "banners", allEntries = true)
    public Banner handleCreate(ReqCreateBannerDTO req, MultipartFile file) throws IOException {
        Banner banner = new Banner();
        banner.setTitle(req.getTitle());
        banner.setSubtitle(req.getSubtitle());
        banner.setDescription(req.getDescription());
        banner.setLink(req.getLink());
        banner.setPosition(req.getPosition());
        banner.setOrder(req.getOrder());
        banner.setActive(req.getIsActive());
        banner.setStartDate(req.getStartDate());
        banner.setEndDate(req.getEndDate());

        if (file != null && !file.isEmpty()) {
            Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
            banner.setImage(uploadResult.get("secure_url").toString());
            banner.setPublicId(uploadResult.get("public_id").toString());
        }

        return this.bannerRepository.save(banner);
    }

    @CacheEvict(value = "banners", allEntries = true)
    public Banner handleUpdate(ReqUpdateBannerDTO req, MultipartFile file) throws IOException {
        if (req == null || req.getId() == null)
            return null;
        Banner cur = this.handleGetById(req.getId());
        if (cur != null) {
            cur.setTitle(req.getTitle());
            cur.setSubtitle(req.getSubtitle());
            cur.setDescription(req.getDescription());
            cur.setLink(req.getLink());
            cur.setPosition(req.getPosition());
            cur.setOrder(req.getOrder());
            cur.setActive(req.getIsActive());
            cur.setStartDate(req.getStartDate());
            cur.setEndDate(req.getEndDate());

            if (file != null && !file.isEmpty()) {
                if (cur.getPublicId() != null) {
                    this.cloudinaryService.deleteFile(cur.getPublicId());
                }
                Map<?, ?> uploadResult = cloudinaryService.uploadFile(file);
                if (uploadResult != null && uploadResult.get("secure_url") != null) {
                    cur.setImage(uploadResult.get("secure_url").toString());
                    cur.setPublicId(uploadResult.get("public_id").toString());
                }
            }

            return this.bannerRepository.save(cur);
        }
        return null;
    }

    @CacheEvict(value = "banners", allEntries = true)
    public Banner handleToggleActive(Long id, Boolean isActive) {
        Banner cur = this.handleGetById(id);
        if (cur != null) {
            cur.setActive(isActive);
            return this.bannerRepository.save(cur);
        }
        return null;
    }

    public Banner handleGetById(Long id) {
        if (id == null)
            return null;
        return this.bannerRepository.findById(id).orElse(null);
    }

    @CacheEvict(value = "banners", allEntries = true)
    public void handleDelete(Long id) throws IOException {
        Banner cur = this.handleGetById(id);
        if (cur != null) {
            if (cur.getPublicId() != null) {
                this.cloudinaryService.deleteFile(cur.getPublicId());
            }
            this.bannerRepository.delete(cur);
        }
    }

    @Cacheable(value = "banners")
    public ResultPaginationDTO handleGetAll(Specification<Banner> spec, Pageable page) {
        Page<Banner> banners = this.bannerRepository.findAll(spec, page);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(banners.getNumber() + 1);
        meta.setPageSize(banners.getSize());
        meta.setPages(banners.getTotalPages());
        meta.setTotal(banners.getTotalElements());

        rs.setMeta(meta);

        // Convert to DTO before caching
        List<ResBannerDTO> dtoList = banners.getContent().stream().map(b -> {
            ResBannerDTO dto = new ResBannerDTO();
            dto.setId(b.getId());
            dto.setTitle(b.getTitle());
            dto.setSubtitle(b.getSubtitle());
            dto.setImage(b.getImage());
            dto.setLink(b.getLink());
            dto.setDescription(b.getDescription());
            dto.setPosition(b.getPosition());
            dto.setOrder(b.getOrder());
            dto.setIsActive(b.getActive());
            dto.setStartDate(b.getStartDate());
            dto.setEndDate(b.getEndDate());
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        rs.setResult(dtoList);
        return rs;
    }
}
