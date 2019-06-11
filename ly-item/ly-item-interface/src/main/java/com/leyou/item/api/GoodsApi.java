package com.leyou.item.api;


import com.leyou.common.dto.CartDTO;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsApi {


    @GetMapping("/spu/detail/{id}")
    SpuDetail querySpuDetailById(@PathVariable("id")Long id);

    @GetMapping("/sku/list")
    List<Sku> querySkuBySpuId(@RequestParam("id") Long spuId);

    @GetMapping("/spu/page")
    PageResult<Spu> querySpuByPage(
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows,
            @RequestParam(value = "saleable",required = false)Boolean saleable,
            @RequestParam(value = "key",required = false)String key
    );

    @GetMapping("spu/{id}")
    Spu querySpuById(@PathVariable("id") Long spuId);

    @GetMapping("/sku/list/ids")
    List<Sku> querySkuByIds(@RequestParam("ids") List<Long> ids);

    @PostMapping("stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> cartDTOS);
}
