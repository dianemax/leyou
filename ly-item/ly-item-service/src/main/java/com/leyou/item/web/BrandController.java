package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.service.BrandService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    /**
     * 分页查询品牌
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @param key
     * @return
     */
    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandByPage(
            @RequestParam(value = "page" , defaultValue = "1") Integer page,
            @RequestParam(value = "rows" , defaultValue = "5") Integer rows,
            @RequestParam(value = "sortBy" , required = false) String sortBy,
            @RequestParam(value = "desc" , defaultValue = "false") boolean desc,
            @RequestParam(value = "key" , required = false) String key

    ){

        PageResult<Brand> result = brandService.queryBrandByPage(page,rows,sortBy,desc,key);
        return ResponseEntity.ok(result);

    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand,@RequestParam("cids")List<Long> cids){
        //用来处理Content-Type: 为 application/x-www-form-urlencoded编码的内容
        //RequestParam可以接受简单类型的属性，也可以接受对象类型。
        //get 方式中queryString的值，和post方式中 body data的值都会被Servlet接受到并转化到
            // Request.getParameter()参数集中，所以@RequestParam可以获取的到
        brandService.saveBrand(brand,cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();//无返回体选择build
    }

    /**
     * 根据cid查询品牌
     * @param cid
     * @return
     */
    @GetMapping("cid/{cid}")
    public ResponseEntity<List<Brand>> queryBrandByCid(@PathVariable("cid")Long cid){
        return ResponseEntity.ok(brandService.queryBrandByCid(cid));
    }

    /**
     * 品牌的删除
     * @param bid
     * @return
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteBrand(@RequestParam(value = "id")Long bid){
        this.brandService.deleteBrand(bid);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    /**
     * 根据品牌id的集合查询所有的品牌
     * @param brandIds
     * @return
     */
    @GetMapping("bids")
    public ResponseEntity<List<Brand>> queryBrandsByBrandIds(@RequestParam("bids") List<Long> brandIds){
        List<Brand> brands = this.brandService.queryBrandsByIds(brandIds);
        if (brands==null||brands.size()<1){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(brands);
    }


    /**
     * 根据商品品牌ID查询品牌
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Brand> queryBrandById(@PathVariable("id")Long id){
        return ResponseEntity.ok(brandService.queryById(id));
    }

    @GetMapping("list")
    public ResponseEntity<List<Brand>> queryBrandsByIds(@RequestParam("ids")List<Long> ids){
        return ResponseEntity.ok(brandService.queryBrandsByIds(ids));
    }

    /**
     *  品牌的修改
     * @param brand
     * @param cids
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateBrand(@RequestParam(value = "cids") List<Long> cids, Brand brand) {
        this.brandService.updateBrand(cids, brand);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
