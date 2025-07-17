package com.liteisle.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResp<T> {
    private Long total;
    private Long currentPage;
    private Long pageSize;
    private List<T> records;
}
