package com.liteisle.service.view.impl;

import com.liteisle.common.dto.response.MusicViewResp;
import com.liteisle.common.exception.LiteisleException;
import com.liteisle.service.core.FilesService;
import com.liteisle.service.core.FoldersService;
import com.liteisle.service.view.MusicViewService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
@Slf4j
@Service
public class MusicViewServiceImpl implements MusicViewService {

    @Resource
    private FilesService filesService;
    @Resource
    private FoldersService foldersService;


    @Override
    public MusicViewResp getMusicView(String content) {
        // 并行执行两个异步查询
        CompletableFuture<List<MusicViewResp.MusicFile>> fileFuture = filesService.getMusicViewWithContent(content);
        CompletableFuture<List<MusicViewResp.Playlist>> folderFuture = foldersService.getMusicViewWithContent(content);

        return CompletableFuture.allOf(fileFuture, folderFuture)
                .thenApply(v -> {
                    try {
                        return new MusicViewResp(
                                folderFuture.get(),  // 获取文件夹列表结果
                                fileFuture.get()     // 获取文件列表结果
                        );
                    } catch (Exception e) {
                        throw new LiteisleException("获取文档信息失败"+e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    log.error("获取文档页面信息失败", ex);
                    return new MusicViewResp(Collections.emptyList(), Collections.emptyList());
                })
                .join();
    }
}
