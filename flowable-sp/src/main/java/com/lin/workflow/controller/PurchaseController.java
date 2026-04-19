package com.lin.workflow.controller;

import com.lin.workflow.common.Result;
import com.lin.workflow.entity.Purchase;
import com.lin.workflow.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/submit")
    public Result<String> submitPurchase(@RequestBody Purchase purchase) {
        purchaseService.submitPurchase(purchase);
        return Result.ok("采购申请已提交！");
    }

    @GetMapping("/my")
    public Result<List<Purchase>> myPurchases(@RequestParam String userId) {
        return Result.ok(purchaseService.listMyPurchases(userId));
    }
}
