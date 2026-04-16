package com.tuna.ecommerce.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.FlashSaleCampaign;
import com.tuna.ecommerce.domain.request.flashsale.ReqFlashSaleCampaignDTO;
import com.tuna.ecommerce.service.FlashSaleService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {
    private final FlashSaleService flashSaleService;

    @PostMapping
    @APIMessage("Tạo chiến dịch Flash Sale thành công")
    public ResponseEntity<FlashSaleCampaign> createCampaign(@RequestBody ReqFlashSaleCampaignDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.flashSaleService.createCampaign(req));
    }

    @GetMapping
    @APIMessage("Lấy danh sách chiến dịch Flash Sale thành công")
    public ResponseEntity<List<FlashSaleCampaign>> getAllCampaigns() {
        return ResponseEntity.ok(this.flashSaleService.getAllCampaigns());
    }

    @DeleteMapping("/{id}")
    @APIMessage("Xóa chiến dịch Flash Sale thành công")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        this.flashSaleService.deleteCampaign(id);
        return ResponseEntity.ok().build();
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @APIMessage("Cập nhật chiến dịch Flash Sale thành công")
    public ResponseEntity<FlashSaleCampaign> updateCampaign(@PathVariable Long id, @RequestBody ReqFlashSaleCampaignDTO req) {
        return ResponseEntity.ok(this.flashSaleService.updateCampaign(id, req));
    }
}
