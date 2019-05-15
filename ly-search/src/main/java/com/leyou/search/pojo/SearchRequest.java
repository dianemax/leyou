package com.leyou.search.pojo;


import java.util.Map;

//前台页面做搜索查询显示分页结果 -- 之前common定义过PageResult
public class SearchRequest {
    private static final Integer DEFAULT_PAGE = 1;
    private static final Integer DEFAULT_SIZE = 20;
    private String key;//搜索条件
    private Integer page;//当前页
    private Integer size=DEFAULT_SIZE;//页面大小

    public void setSize(Integer size) {
        this.size=DEFAULT_SIZE;
    }

    //排序字段
    private String sortBy;
    //是否降序
    private Boolean descending;

    //过滤字段
    private Map<String, String> filter;

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Boolean getDescending() {
        return descending;
    }

    public void setDescending(Boolean descending) {
        this.descending = descending;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getPage() {
        if (page == null) {//默认为1
            return DEFAULT_PAGE;
        }
        // 获取页码时做一些校验，不能小于1
        return Math.max(DEFAULT_PAGE, page);
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public Map<String, String> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, String> filter) {
        this.filter = filter;
    }
}