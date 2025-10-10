package com.zhao.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageBean <T>{
    private Integer page;//页数
    private Integer pageSize;//每页展示的条数
    private  Long total;//总条数
    private List<T> item;//当前页数据集合


    public PageBean(List<T> item, Long total, Integer page, Integer pageSize) {
        this.item = item;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }
}
