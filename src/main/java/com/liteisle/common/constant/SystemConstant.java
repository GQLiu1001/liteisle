package com.liteisle.common.constant;

import java.util.List;

public class SystemConstant {
    public static final String USER_DEFAULT_URL =
            "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/liteisledefaultuserpic.png";

    public static final Long USER_DEFAULT_STORAGE_QUOTA = 5368709120L;

    /**
     * 用户头像的存储路径前缀。
     * 格式: avatars/{userId}/
     */
    public static final String USER_AVATAR_FOLDER_PREFIX = "avatars/%d/";

    /**
     * DATA_BUCKET_PREFIX - 哈希文件存储目录。
     * 所有文件根据其内容的哈希值存放，实现系统级去重。
     * 示例: data/e3/b0/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
     */
    public static final String DATA_BUCKET_PREFIX = "data/";

    public static final String AVATAR_FILE_NAME = "avatar"; // 头像文件名可以保持不变

    public static final List<String> SUPPORTED_AVATAR_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".gif");

    public static final List<String> DEFAULT_SYSTEM_FOLDERS =
            List.of("歌单", "书单", "上传", "分享");

    public static final int MAX_TOKEN_ATTEMPTS = 10;
}