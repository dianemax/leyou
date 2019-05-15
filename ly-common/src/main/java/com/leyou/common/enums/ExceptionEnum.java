package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ClassName:ExceptionEnums
 * PackageName:com.leyou.common.enums
 * Description:
 *
 * @Date:2019/3/16 20:51
 * @Author:dianemax@163.com
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
//@NoArgsConstructor: 自动生成无参数构造函数。
//@AllArgsConstructor: 自动生成全参数构造函数。

public enum ExceptionEnum {

    PRICE_CANNOT_BE_NULL(400,"价格不能为空!"),
    CATEGORY_NOT_FOUND(404,"商品分类没查到！"),
    BRAND_NOT_FOUND(400,"该品牌未找到！" ),
    BRAND_CREATE_FAILED(500,"新增品牌失败！" ),
    CONTENT_TYPE_ERROE(400,"文件类型无效！" ),
    INVALID_IMAGE_ERROE(400,"文件内容无效！" ),
    UPLOAD_FAILED_ERROE(400, "文件上传失败！"),
    GROUP_NOT_FOUND(404,"商品规格组不存在！"),
    SPU_DETAIL_NOT_FOUND(404,"商品规格组不存在！"),
    SKU_NOT_FOUND(404,"SKU商品不存在！"),
    STOCK_NOT_FOUND(404,"SKU商品不存在！"),
    GROUP_PARAM_NOT_FOUND(404,"商品规格组不存在！"),
    SPU_NOT_FOUND(404,"商品不存在！" ),
    SAVE_GOOD_ERROR(500,"商品保存失败！"),
    SPEC_PARAM_NOT_FOUND(400,"规格参数没查到！" ),
    GOODS_UPDATE_ERROR(400,"商品更新失败！" ),
    INVALID_PARAM(400,"无效的参数！" ),
    GOODS_SAVE_ERROR(400,"商品保存失败！" ),
    SPEC_GROUP_CREATE_FAILED(400,"商品组生成失败！" ),
    DELETE_SPEC_GROUP_FAILED(400,"删除商品组失败！" ),
    UPDATE_SPEC_GROUP_FAILED(400,"更新商品组失败！" ),
    SPEC_PARAM_CREATE_FAILED(400,"特殊规格参数生成失败！" ),
    UPDATE_SPEC_PARAM_FAILED(400,"特殊规格参数更新失败！" ),
    DELETE_SPEC_PARAM_FAILED(400,"特殊规格参数删除失败！" ),
    INVALID_USER_DATA_TYPE(400,"用户数据类型无效！" ),
    INVALID_VERIFY_CODE(400,"验证码校验失败！" ),
    INVALID_USERNAME_PASSWORD(400,"用户名或密码错误！" ),
    CREATE_TOKEN_ERROR(500,"用户凭证生成失败！" ),
    NO_AUTHORIZED(401,"没有登录权限！" ),
    CREATE_ORDER_ERROR(500,"创建订单失败！" ),
    STOCK_NOT_ENOUGH(400,"库存不足！" ),
    ORDER_NOT_FOUND(404,"订单不存在！" ),
    ORDER_DETAIL_NOT_FOUNT(404,"订单详情不存在！" ),
    ORDER_STATUS_NOT_FOUND(404,"订单状态不存在！" ),
    WX_PAY_ORDER_FAIL(500,"微信下单失败！" ),
    ORDER_STATUS_ERROE(500,"订单状态异常！" ),
    INVALID_ORDER_PARAM(400,"订单参数异常！" ),
    UPDATE_ORDER_STATUS_ERROR(400,"订单状态更新失败！" );
    private int code;
    private String msg;

}
