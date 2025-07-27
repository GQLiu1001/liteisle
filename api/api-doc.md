# API文档

### **1. 通用规范**

#### **1.1. 基础 URL**

所有API的根路径为 `https://api.liteisle.com/v1` (示例)。

#### **1.2. JSON 命名规范**

- **API 层面**: 所有通过 API 传输的 JSON 对象的键（key）均采用**蛇形命名法 (snake_case)**。
- **后端实现**: 后端 Spring Boot 应用通过 Jackson 配置，自动将 API 接口的 `snake_case` 与 Java 代码中的 `camelCase` 进行转换，后端开发者在代码中应使用驼峰命名法。

#### **1.3. 统一响应格式**

所有API在成功或失败时均返回以下结构的JSON对象。

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

| 字段      | 类型                  | 描述                                                         |
| :-------- | :-------------------- | :----------------------------------------------------------- |
| `code`    | Integer               | 业务状态码。200: 成功, 202: 已接收(用于异步), 400: 客户端错误, 401: 未认证, 403: 无权限, 500: 服务器内部错误。 |
| `message` | String                | 对操作结果的文字描述，用于调试或前端提示。                   |
| `data`    | Object / Array / null | 接口返回的核心业务数据。                                     |

**通用成功响应示例**

当接口没有特定业务数据返回时（如删除、更新操作），`data` 字段为 `null`。

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

#### **1.4. 实时通信 (WebSocket)**

本应用采用 WebSocket 进行实时双向通信，以提供即时的数据更新和通知。

**连接端点:** `WS /ws`

**消息格式:**

```json
{ 
    "event": "event_name", 
 	"payload": { ... } 
}
```

**核心服务端推送事件 (S2C - Server to Client):**

*   `file.status.updated`: 文件后台处理状态更新。当文件从 `processing` 变为 `available` 时推送。
*   `transfer.log.updated`: 传输日志状态更新。
*   `notification.new`: 新通知。

#### 1.5 项目要点

- 桌面端应用，使用electron打包。
- 涉及到下载、上传都会在传输页面创建任务。
- ShareLinks 和 取消上传任务都是硬删除。
- md文件不占用户空间 md里的图片保存在客户端

---

### **2. 认证 (Authentication)**

#### **2.1. 用户登录**

`POST /auth/login`

**描述:** 用户通过用户名和密码进行登录，成功后返回用户信息和Token。

**请求体:** `@RequestBody AuthLoginReq`

```json
{
  "username": "john_doe",
  "password": "your_strong_password"
}
```

**响应:** `AuthInfoResp`

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "username": "john_doe",
    "email": "john@example.com",
    "avatar": "https://r2.liteisle.com/pictures/default.png",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
  }
}
```

#### **2.2. 用户注册**

`POST /auth/register`

**描述:** 用户注册新账号，注册成功后即为登录状态。

**请求体:** `@RequestBody AuthRegisterReq`

```json
{
  "username": "new_user",
  "email": "new_user@example.com",
  "password": "your_strong_password",
  "vcode": "123456"
}
```

**响应:** `AuthInfoResp` (与登录成功响应结构相同)

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "username": "new_user",
    "email": "new_user@example.com",
    "picture": "https://r2.liteisle.com/pictures/default.png",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6Ik5ldyBVc2VyIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
  }
}
```

#### **2.3. 发送验证码**

`POST /auth/send-vcode`

**描述:** 向指定邮箱发送验证码，用于注册或忘记密码。

**查询参数:**

| 参数    | 类型   | 是否必须 | 描述                   |
| :------ | :----- | :------- | :--------------------- |
| `email` | String | 是       | 接收验证码的邮箱地址。 |

**响应:**

```json
{
  "code": 200,
  "message": "验证码已发送，请注意查收",
  "data": null
}
```

#### **2.4. 忘记密码**

`POST /auth/forgot-password`

**描述:** 用户通过邮箱验证码重置密码。

**请求体:** `@RequestBody AuthForgotPasswordReq`

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "vcode": "123456",
  "new_password": "new_secure_password",
  "confirm_password": "new_secure_password"
}
```

**响应:**

```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": null
}
```

#### **2.5. 获取当前用户信息**

`GET /auth/me`

**描述:** 获取当前登录用户的信息，用于“设置”页面展示。

**响应:** `AuthCurrentUserResp`

```json
{
  "code": 200,
  "message": "获取用户信息成功",
  "data": {
    "username": "john_doe",
    "email": "john@example.com",
    "avatar": "https://r2.liteisle.com/pictures/12345.jpg",
    "storage_used": 2147483648,
    "storage_quota": 5368709120
  }
}
```

> **备注:** `storage_used` 和 `storage_quota` 单位为字节 (bytes)。

#### **2.6. 修改当前用户密码**

`POST /auth/me/reset-password`

**描述:** 登录状态下，用户通过旧密码修改新密码。

**请求体:** `@RequestBody AuthResetPasswordReq`

```json
{
  "old_password": "your_strong_password",
  "new_password": "new_secure_password",
  "confirm_password": "new_secure_password"
}
```

**响应:**

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

#### **2.7. 上传当前用户头像**

`POST /auth/me/picture`

**描述:** 上传并更新当前用户的头像。

**请求体:** `multipart/form-data`

| 字段名 | 类型 | 描述                     |
| :----- | :--- | :----------------------- |
| `file` | File | 用户选择的头像图片文件。 |

**响应:**

```json
{
  "code": 200,
  "message": "头像上传成功",
  "data": "https://r2.liteisle.com/pictures/67890.jpg"
}
```

#### **2.8. 重置当前用户头像**

`PUT /auth/me/reset-picture`

**描述:** 将当前用户的头像重置为系统默认头像。

**请求体:** 无

**响应:**

```json
{
  "code": 200,
  "message": "头像已重置",
  "data": "https://r2.liteisle.com/pictures/default.png"
}
```

#### **2.9. 退出登录**

`POST /auth/logout`

**描述:** 后端清除用户认证凭证。前端应配合删除本地存储的Token。

**请求体:** 无

**响应:**

```json
{
  "code": 200,
  "message": "已成功退出登录",
  "data": null
}
```

---

### **3. 文件与文件夹 (File & Folder)**

#### **3.1. 获取指定文件夹内容**

`GET /folders/{folder_id}`

**描述:** 获取指定文件夹下的所有文件夹和文件列表。这是云盘主界面的核心接口。

**路径参数:**

| 参数        | 类型 | 描述                                   |
| :---------- | :--- | :------------------------------------- |
| `folder_id` | Long | 文件夹ID。**特别地，`0` 代表根目录**。 |

**查询参数:**

| 参数         | 类型   | 是否必须 | 描述                                                         |
| :----------- | :----- | :------- | :----------------------------------------------------------- |
| `sort_by`    | String | 否       | 排序字段，可选值: `name`, `file_size`, `create_time`, `update_time`, `sorted_order` (默认) |
| `sort_order` | String | 否       | 排序选项，可选`ASC`,`DEC`默认`DEC`                           |
| `content`    | String | 否       | 匹配文件/文件夹名字字段                                      |

**响应:** `FolderContentResp`

```json
{
  "code": 200,
  "message": "获取列表成功",
  "data": {
      "breadcrumb": [
  	{ 
        "id": 1, 
        "name": "文档" 
    }
	],
    "folders": [
      {
        "id": 101,
        "folder_name": "我的文档",
        "folder_type": "booklist",//枚举 FolderTypeEnum
        "sub_count": 3,
        "sorted_order": 1704186000000.123124124124,
        "create_time": "2024-01-02T09:00:00Z",
        "update_time": "2024-01-02T10:30:00Z"
      }
    ],
    "files": [
      {
        "id": 201,
        "file_name": "项目报告.pdf",
        "file_type": "document",//枚举 FileTypeEnum
        "file_size": 2097152,
        "file_status": "available",//枚举 FileStatusEnum
        "sorted_order": 1704186000321000.123124124124,
        "create_time": "2024-01-02T09:00:00Z",
        "update_time": "2024-01-02T09:00:00Z"
      },
      {
        "id": 202,
        "file_name": "临时录音.mp3",
        "file_type": "music",
        "file_size": 0,
        "file_status": "processing",
        "sorted_order": 1704186002140000.123124124124,
        "create_time": "2024-01-02T09:10:00Z",
        "update_time": "2024-01-02T09:10:00Z"
      }
    ]
  }
}
```

> **备注:**
>
> * `folder_type`: 枚举 `system`(系统), `playlist`(歌单), `booklist`(笔记本)。
> * `file_type`: 枚举 `music`, `document` 等。
> * `status`: 文件状态，`available` (可用), `processing` (处理中)。前端应根据此状态显示不同UI。
> * `sub_item_count`: 文件夹下子项目的数量（文件+文件夹）。
> * **WebSocket 交互**: 当 `status` 为 `processing` 的文件处理完成时，服务器将通过WebSocket推送 `file.status.updated` 事件，前端应监听此事件并更新对应文件的UI，无需重新拉取整个列表。

#### **3.2. 创建文件夹**

`POST /folders`

**描述:** 在指定的父文件夹下创建新文件夹。

**请求体:** `@RequestBody FolderCreateReq`

```json
{
  "name": "新建文件夹",
  "parent_id": 101,
  "folder_type": "playlist" // 明确指定类型 FolderTypeEnum
}
```

> **注意:** `parent_id` 不能为 `0`，根目录下不允许用户直接创建文件夹，系统文件夹由系统管理。

**响应:**

```json
{
  "code": 200,
  "message": "文件夹创建成功",
  "data": null
}
```

#### **3.3. 重命名项目 (文件/文件夹)**

`PUT /items/rename`

**描述:**
重命名一个指定类型（文件或文件夹）的项目。

**请求体:** `@RequestBody ItemsRenameReq`

```json
{
  //前端发送id
  "file_id": null,
  "folder_id": 3,
  "new_name": "新的项目名称.mp3" 
}
```

**响应:**

```json
{
  "code": 200,
  "message": "重命名成功",
  "data": null
}
```

#### **3.4. 移动项目 (文件/文件夹)**

`PUT /items/move`

**描述:** 将一个或多个文件/文件夹移动到新的目标文件夹。

**请求体:** `@RequestBody ItemsOperationReq`

```json
{
  "file_ids": [201],
  "folder_ids": [102],
  "target_folder_id": 3
}
```

**响应:**

```json
{
  "code": 200,
  "message": "移动成功",
  "data": null
}
```

#### **3.5. 删除项目 (移入回收站)**

`DELETE /items`

**描述:** 将一个或多个文件/文件夹移入回收站（软删除 不影响用户额度）。

**请求体:** `@RequestBody ItemsDeleteReq `

```json
{
  "file_ids": [201],
  "folder_ids": [102]
}
```

**响应:**

```json
{
  "code": 200,
  "message": "已将 2 个项目移入回收站",
  "data": null
}
```

#### **3.6. 获取项目详情 (文件/文件夹)**

`GET /items/{item_id}/detail`

**描述:** 获取单个文件或文件夹的详细信息。

**路径参数:**

| 参数    | 类型 | 描述                |
| ------- | ---- | ------------------- |
| item_id | Long | 文件或文件夹的 ID。 |

请求参数:

| 参数      | 类型   | 描述           |
| --------- | ------ | -------------- |
| item_type | String | file或者folder |

**响应:** `ItemDetailResp` (返回统一结构，通过 item_type 区分)

```json
{
  "code": 200,
  "message": "获取详情成功",
  "data": {
    "id": 201,
    "name": "项目报告.pdf",
    "item_type": "file", // "file" 或 "folder" ItemType枚举类
    "size": 2097152, // 文件夹此项为 0 或其内容个数
    "path": "/我的文档/项目报告.pdf",
    "create_time": "2024-01-02T09:00:00Z",
    "update_time": "2024-01-02T09:00:00Z"
  }
}
```

#### **3.7. 获取所有文件夹层级 (用于移动对话框)**

`GET /folders/hierarchy`

**描述:** 获取用户所有文件夹的树状层级结构，用于“移动到...”功能的对话框。

**响应:** `List<FolderHierarchyResp>`

```json
{
  "code": 200,
  "message": "获取文件夹层级成功",
  "data": [
    {
      "id": 1,
      "folder_name": "音乐",
      "folder_type": "system", //FolderTypeEnum
      "parent_id": 0
    },
    {
      "id": 3,
      "folder_name": "我喜欢的音乐",
      "folder_type": "playlist",
      "parent_id": 1
    },
    {
      "id": 101,
      "folder_name": "我的文档",
      "folder_type": "booklist",
      "parent_id": 0
    }
  ]
}
```

> **备注:** `parent_id` 为 `0` 代表根目录下的文件夹。

#### **3.8. 复制项目 (文件/文件夹)**

`POST /items/copy`

**描述:** 将一个或多个文件/文件夹复制到新的目标文件夹（影响用户额度）。

**请求体:** `@RequestBody ItemsOperationReq`

```json
{
  "file_ids": [201],
  "folder_ids": [102],
  "target_folder_id": 3
}
```

**响应:**

```Json
{
  "code": 200,
  "message": "复制成功",
  "data": null
}
```

#### **3.9. 自定义排序 (文件/文件夹)**

`PUT /items/{item_id}/set-order`

**描述:** 在特定视图（如音乐页面、文档页面）中拖拽项目（歌曲、文档、歌单、笔记本）进行排序。

请求参数:

| 参数      | 类型   | 描述           |
| --------- | ------ | -------------- |
| item_type | String | file或者folder |

**路径参数:**

| 参数    | 类型 | 描述                        |
| ------- | ---- | --------------------------- |
| item_id | Long | 要移动的文件或文件夹的 ID。 |

**请求体:** `@RequestBody SetOrderReq`

```Json
{
  // 提供被移动到的位置，其前一个和后一个项目的sorted_order值
  // 如果移到最前，after_order为null；如果移到最后，before_order为null
  "before_id": 3,
  "after_id": 6
}
```

**响应:**

```Json
{
  "code": 200,
  "message": "排序已更新",
  "data": null
}
```

---

### 4. 文件上传与下载

#### 4.1. 上传文件 (异步处理)

`POST /upload`

> **说明**: 如果folder_id为0则是为传输页面的上传任务，后端自动获取当前用户的上传系统文件夹id存入

**描述:** 接收文件并立即返回，将耗时的文件处理（如元数据提取）交由后台异步完成。.md 文档将直接存储文本内容到数据库，无需上传到对象存储（影响用户额度）。

**请求体:** `multipart/form-data`

| 字段名 | 类型 | 是否必须 | 描述           |
| :----- | :--- | :------- | :------------- |
| `file` | File | 是       | 要上传的文件。 |

请求参数

| 字段名      | 类型 | 是否必须 | 描述                           |
| :---------- | :--- | :------- | :----------------------------- |
| `folder_id` | Long | 是       | 用户自定义要上传到的文件夹id。 |

**响应 (快速返回):** `FileUploadAsyncResp`

```json
{
  "code": 202,
  "message": "文件已接收，正在后台处理",
  "data": {
    "log_id": 123, // ✨ 新增
    "file_id": 203,
    "log_status": "processing",//枚举 log状态TransferStatusEnum
    "initial_file_data": {
        "id": 203,
        "name": "晴天.mp3",
        "file_type": "music",//枚举 文件类型 
        "file_status": "processing",  //
        "sorted_order": 1704186900000.19712414,
        "create_time": "2024-01-02T09:15:00Z",
        "update_time": "2024-01-02T09:15:00Z"
    }
  }
}
```

> **流程说明:**
>
> 1. 前端收到此 `202 Accepted` 响应后，应立即使用 `initial_file_data` 在UI中渲染出文件条目，并显示为“处理中”状态。
> 2. 后端Worker处理完毕后，会通过 WebSocket 推送一个 `file.status.updated` 事件。
> 3. 前端监听此事件，根据 `file_id` 找到对应的UI元素，并使用事件 `payload` 中的完整数据更新该文件，将其状态更新为 `available`。
> 4. **后端职责说明**:
     >    服务器在文件处理完毕后（如元数据提取完成），应**自动更新**关联的传输日志(transfer_log)状态从 processing 到 success 或 failed。前端无需再次调用接口来更新上传任务的状态。状态变更会通过 WebSocket (transfer.log.updated) 推送给客户端。

#### **4.2. 下载文件 (全新简化流程)**

`POST /download/create`

> 注意：下载根目录文件夹如歌单，需要前端提供多个子文件夹，后端实现：一级文件夹/二级文件如：歌单/我喜欢的 以及 歌单/我喜欢的/东风破.mp3

**描述:** 所有**下载到本地**操作的**唯一入口**。客户端提交要下载的项目，后端负责：

1. 递归解析所有文件。
2. 为每个下载任务**自动创建传输日志记录**。
3. 为每个文件生成带签名的下载URL。
4. 将包含所有必要信息的完整清单一次性返回给客户端。

**请求体:** `@RequestBody ItemsSelectionReq`

```json
// 下载单个文件、多个文件、一个文件夹
// 例：前端框选 文件夹3、文件夹4、文件6，触发三次请求分别为文件夹3、文件夹4、文件6.
//  如果前端选择 文件456则触发一次请求
{
  "file_ids": [201, 202, 203],
  "folder_id": null  // 单个文件夹ID
}
```

**响应:** `DownloadSessionResp`

```json
{
  "code": 200,
  "message": "下载会话创建成功",
  "data": {
    "total_size": 31457280,
    "total_files": 5,
    "folder_d": {
        "folder_id": 3,
        "folder_name": "文档",
        "relative_path": "我的项目/文档" 
    },
    "files_d": [
      {
        "log_id": 123, // ✨ 后端创建的日志ID
        "file_id": 202,
        "file_name": "会议记录.docx",
        "relative_path": "我的项目/文档/会议记录.docx",
        "size": 20480,
        "download_url": "https://r2.liteisle.com/downloads/a1b2...?token=..." // ✨ 直接返回URL
      },
      {
        "log_id": 124,
        "file_id": 201,
        "file_name": "封面图.pdf",
        "relative_path": "我的项目/文档/封面图.pdf",
        "size": 512000,
        "download_url": "https://r2.liteisle.com/downloads/b2c3...?token=..."
      }
      // ...更多文件
    ]
  }
}
```

```json
{
  "code": 200,
  "message": "下载会话创建成功",
  "data": {
    "total_size": 512000,
    "total_files": 1,
    "folder_d": null,  // 单个文件下载时无文件夹信息
    "files_d": [
      {
        "log_id": 124,
        "file_id": 201,
        "relative_path": "封面图.jpg",  // 无父文件夹，直接为文件名
        "size": 512000,
        "download_url": "https://r2.liteisle.com/downloads/b2c3...?token=..."
      }
    ]
  }
}
```

1. 前端请求：分为单个文件夹/文件请求。
2. **执行下载**：客户端收到 DownloadSessionResp 后，遍历 items 数组。
    - 如果 type 是 directory，则在本地创建对应目录。
    - 如果 type 是 file，则使用提供的 download_url 下载文件到 relative_path。
3. **更新状态**：在每个文件下载结束时（成功、失败、取消），客户端调用 PUT /transfers/{log_id}/status (见 5.5 节) 来更新该任务的最终状态。

### **5. 传输管理 (Transfer Management)**

#### **5.1. 获取传输任务摘要**

`GET /transfers/summary`

**描述:** 获取正在进行中的上传和下载任务总数，用于侧边栏角标显示。

**响应:** `TransferSummaryResp`

```json
{
  "code": 200,
  "message": "获取摘要成功",
  "data": {
    "upload_count": 2,
    "download_count": 1
  }
}
```

#### **5.2. 获取传输历史记录**

`GET /transfers`

**描述:** 分页获取传输记录，用于“传输列表”页面。

**查询参数:**

| 参数     | 类型    | 是否必须 | 描述                                                         |
| :------- | :------ | :------- | :----------------------------------------------------------- |
| `status` | String  | 是       | `processing`: 进行中, `completed`: 已完成(含成功和失败)对应两个页面（进行中和已完成）。 |
| `page`   | Integer | 否       | 页码，默认 1。                                               |
| `size`   | Integer | 否       | 每页数量，默认 20。                                          |

**响应:** `TransferLogPageResp`

```json
{
  "code": 200,
  "message": "获取传输记录成功",
  "data": {
    "total": 25,
    "current_page": 1,
    "page_size": 20,
    "records": [
      {
        "log_id": 1,
        "item_name": "项目报告.pdf",
        "item_size": 2097152,
        "transfer_type": "upload", //TransferTypeEnum
        "create_time": "2024-07-25T10:00:00Z"
      },
      {
        "log_id": 2,
        "item_name": "晴天.mp3",
        "item_size": 5242880,
        "transfer_type": "upload",//TransferTypeEnum
        "create_time": "2024-07-25T09:55:00Z"
      }
    ]
  }
}
```

#### 5.3. 删除已完成的单条传输记录

`DELETE /transfers/{log_id}`

**描述:** 从传输列表中删除一条记录（如果选择删除文件也是软删除 不影响用户额度）。

**路径参数:**

| 参数     | 类型 | 描述                 |
| :------- | :--- | :------------------- |
| `log_id` | Long | 要删除的传输记录ID。 |

**查询参数:**

| 参数          | 类型    | 是否必须 | 描述                                                 |
| :------------ | :------ | :------- | :--------------------------------------------------- |
| `delete_file` | Boolean | 否       | 是否同时将关联的文件移入回收站。**默认为 `false`**。 |

**响应:**

```json
{
  "code": 200,
  "message": "传输记录已删除",
  "data": null
}
```

#### 5.4. 清空已完成的传输记录

`DELETE /transfers/completed`

**描述:** 清空所有已完成（成功或失败）的传输记录。**此操作仅删除日志，不影响实际文件（可选）**。注意对于下载记录只删除记录（如果选择删除文件也是软删除 不影响用户额度）。

**查询参数:**

| 参数          | 类型    | 是否必须 | 描述                                                 |
| :------------ | :------ | :------- | :--------------------------------------------------- |
| `delete_file` | Boolean | 否       | 是否同时将关联的文件移入回收站。**默认为 `false`**。 |

**响应:**

```json
{
  "code": 200,
  "message": "已清空23条已完成的记录",
  "data": null
}
```

#### ！5.5. 取消上传任务

`POST /transfers/upload/{log_id}/cancel`

**描述:** 取消一个正在进行的上传任务。此操作会中断文件传输，并清理服务器上已接收的该文件的临时数据（恢复用户额度）。

**路径参数:**

| 参数     | 类型 | 描述                                                         |
| -------- | ---- | ------------------------------------------------------------ |
| `log_id` | Long | 要取消的上传任务的传输日志ID。这个ID在调用POST /upload或POST /shares/save时，可以随initial_file_data一起返回，或者通过查询传输列表GET /transfers获取。 |

**响应:**

```JSON
{
  "code": 200,
  "message": "上传任务已取消",
  "data": null
}
```

#### **5.6. 更新下载任务状态 (职责明确)**

`PUT /transfers/{log_id}/status`

**描述:** 由**客户端**在**下载**任务结束（成功、失败、取消）后，或**任何由客户端发起的取消操作**后调用，以更新其在后端的日志状态。

**路径参数:**

| 参数   | 类型 | 描述             |
| ------ | ---- | ---------------- |
| log_id | Long | 要更新的日志ID。 |

**请求体:** `@RequestBody TransferStatusUpdateReq`

```Json
{
  "log_status": "success", // "success", "failed", "canceled"
  "error_message": null,
  "transfer_duration_ms": 15000
}
```

**响应:**

```Json
{ 
    "code": 200, 
    "message": "状态更新成功", 
    "data": null 
}
```

---

### **6. 特定视图 (Special Views)**

此部分API专为特定的前端视图（如音乐播放器、文档管理器）提供聚合数据，以优化性能和简化前端逻辑。

#### **6.1. 音乐视图 (Music View)**

##### **6.1.1. 获取音乐页面信息**

`GET /music`

**描述:** 获取音乐页面所需的所有数据，包括所有播放列表（Playlist）和对应播放列表的音乐文件。此接口用于页面初始化，前端可将数据存入状态管理库（Store）。

**查询参数:**

| 参数      | 类型   | 是否必须 | 描述                                                       |
| :-------- | :----- | :------- | :--------------------------------------------------------- |
| `content` | String | 否       | 搜索关键词。如果提供，将返回与关键词匹配的播放列表和歌曲。 |

**响应:** `MusicViewResp`

```json
{
  "code": 200,
  "message": "获取音乐列表成功",
  "data": {
    "playlists": [
      {
        "id": 101,
        "name": "我喜欢的音乐",
        "folder_type": "playlist",//enum
        "sorted_order": 1704186000000,
        "sub_count": 2
      }
    ],
    "files": [
      {
        "id": 205,
        "folder_id": 101,
        "file_name": "晴天.mp3",
        "file_type": "music",//enum
        "sorted_order": 1704186100000,
        "artist": "周杰伦",
        "album": "叶惠美",
        "duration": 269,
        "cover_art_url": "https://r2.liteisle.com/covers/yehuimei.jpg"
      },
      {
        "id": 206,
        "folder_id": 101,
        "file_name": "稻香.mp3",
        "file_type": "music",
        "sorted_order": 1704186200000,
        "artist": "周杰伦",
        "album": "魔杰座",
        "duration": 223,
        "cover_art_url": "https://r2.liteisle.com/covers/mojiezuo.jpg"
      }
    ]
  }
}
```

> **备注:** `duration` 单位为秒 (seconds)。

##### **6.1.2. 获取音乐播放链接**

`GET /music/{file_id}/play`

**描述:** 获取指定音乐文件的临时、安全的播放流链接。

**路径参数:**

| 参数      | 类型 | 描述           |
| :-------- | :--- | :------------- |
| `file_id` | Long | 音乐文件的ID。 |

**响应:**

```json
{
  "code": 200,
  "message": "获取音乐链接成功",
  "data": "https://r2.liteisle.com/music/asdqwd.mp3?token=..."
}
```

#### **6.2. 文档视图 (Document View)**

##### **6.2.1. 获取文档页面信息**

`GET /documents`

**描述:** 获取文档页面所需的所有数据，包括所有笔记本（booklist）和所有文档文件。

**查询参数:**

| 参数      | 类型   | 是否必须 | 描述                                                     |
| :-------- | :----- | :------- | :------------------------------------------------------- |
| `content` | String | 否       | 搜索关键词。如果提供，将返回与关键词匹配的笔记本和文档。 |

**响应:** `DocumentViewResp`

```json
{
  "code": 200,
  "message": "获取文档列表成功",
  "data": {
    "booklists": [
      {
        "id": 301,
        "folder_name": "会议纪要",
        "folder_type": "booklist",//enum
        "sorted_order": 1704187000000.83278441,
        "sub_count": 1
      }
    ],
    "files": [
      {
        "id": 401,
        "folder_id": 301,
        "file_name": "3-18周会.md",
        "file_type": "document",//enum
        "sorted_order": 1704187100000.8712367812412
      },
      {
        "id": 402,
        "folder_id": 301,
        "file_name": "产品需求文档.pdf",
        "file_type": "document",
        "sorted_order": 170134187200000.123124
      }
    ]
  }
}
```

##### **6.2.2. 获取非 MD 文档预览/下载链接**

`GET /documents/{file_id}/view`

**描述:** 获取非 Markdown 文档（如 PDF, DOCX）的预览或下载链接。

**路径参数:**

| 参数      | 类型 | 描述         |
| :-------- | :--- | :----------- |
| `file_id` | Long | 文档文件的ID |

**响应:**

```json
{
  "code": 200,
  "message": "获取文档链接成功",
  "data": "https://r2.liteisle.com/docs/asdqwd.pdf?token=..."
}
```

##### **6.2.3. 获取 MD 文档内容**

`GET /documents/md/{file_id}`

**描述:** 获取指定 Markdown 文件的原始内容，用于在线编辑器加载。

**路径参数:**

| 参数      | 类型 | 描述                |
| :-------- | :--- | :------------------ |
| `file_id` | Long | Markdown 文件的ID。 |

**响应:** `MarkdownContentResp`

```json
{
  "code": 200,
  "message": "获取Markdown文档成功",
  "data": {
    "content": "# 周会纪要\n\n- 讨论了Q2季度目标。\n- 分配了新功能开发任务。",
    "version": 5 // 版本号
  }
}
```

##### **6.2.4. 更新并保存 MD 文档**

`PUT /documents/md/{file_id}`

**描述:** 更新并保存在线编辑器中修改后的 Markdown 文档内容（md文件不影响用户额度）。

**路径参数:**

| 参数      | 类型 | 描述                         |
| :-------- | :--- | :--------------------------- |
| `file_id` | Long | 要保存的 Markdown 文件的ID。 |

**请求体:** `@RequestBody MarkdownUpdateReq`

```json
{
    "content": "# 周会纪要 (更新)\n\n- 讨论了Q2季度目标。\n- 分配了新功能开发任务。\n- **补充：** 下周三进行演示。",
    "version": 5 // ✨ 前端从GET请求中获取并回传的版本号
}
```

**响应:**

```json
{
  "code": 200,
  "message": "文档保存成功",
  "data": null
}
```

##### **6.2.5. 新建 MD 文档**

`POST /documents/md`

**描述:** 在指定的笔记本下创建一个新的、空的 Markdown 文档（md文件不影响用户额度）。

**请求体:** `@RequestBody MarkdownCreateReq`

```json
{
  "name": "未命名文档.md",
  "folder_id": 301 // 要放入的笔记本ID
}
```

**响应:**

```json
{
  "code": 200,
  "message": "文档新建成功",
  "data": 31
}
```

> **备注:** 返回参数为新创建文档的id。

##### **6.2.6.获取MD文档版本号**

`GET /documents/md-version/{file_id}`

**描述:** 前端在触发保存获取到200成功后 请求更新版本号以便下次保存时传递。

**路径参数:**

| 参数      | 类型 | 描述                         |
| :-------- | :--- | :--------------------------- |
| `file_id` | Long | 要保存的 Markdown 文件的ID。 |

**响应:**

```json
{
  "code": 200,
  "message": "获取成功",
  "data": 31
}
```

> **备注:** 返回参数为md版本号

---

### **7. 回收站 (Recycle Bin)**

#### **7.1. 获取回收站内容**

`GET /recycle-bin`

**描述:** 获取当前用户回收站中的所有文件和文件夹。

请求参数：

| 参数      | 类型   | 描述 |
| :-------- | :----- | :--- |
| `content` | String | 字段 |

**响应:** `RecycleBinContentResp`

```json
{
  "code": 200,
  "message": "获取回收站列表成功",
  "data": {
    "folders": [
      {
        "original_id": 102,
        "original_name": "旧项目",
        "original_type": "playlist", //FolderTypeEnum
        "sub_count": 3,
        "delete_time": "2024-07-20T11:00:00Z",
        "expire_time": "2024-08-19T11:00:00Z"
      }
    ],
    "files": [
      {
        "original_id": 208,
        "original_name": "草稿.txt",
        "original_type": "document", //FileTypeEnum
        "file_size": 1024,
        "delete_time": "2024-07-21T15:30:00Z",
        "expire_time": "2024-08-20T15:30:00Z"
      }
    ]
  }
}
```

> **备注:** `expire_time` 表示项目将被系统自动永久删除的时间。

#### **7.2. 还原项目**

`POST /recycle-bin/restore`

**描述:** 将回收站中的一个或多个项目还原到其原始位置（不影响用户额度）。

**请求体:** `@RequestBody RecycleBinReq`

```json
{
  "file_ids": [208],
  "folder_ids": [102]
}
```

**备注:** 请求体中明确区分 file_ids 和 folder_ids 数组，两者至少提供一个。

**响应:**

```json
{
  "code": 200,
  "message": "已成功还原 2 个项目",
  "data": null
}
```

#### **7.3. 彻底删除项目**

`DELETE /recycle-bin/items`

**描述:** 从回收站中永久删除一个或多个项目。此操作不可恢复（恢复用户额度）。

**请求体:** `@RequestBody RecycleBinReq`

```json
{
  "file_ids": [208],
  "folder_ids": [102]
}
```

**响应:**

```json
{
  "code": 200,
  "message": "已彻底删除 2 个项目",
  "data": null
}
```

#### **7.4. 清空回收站**

`DELETE /recycle-bin/all`

**描述:** 永久删除回收站中的所有项目。此操作不可恢复（恢复用户额度）。

**响应:**

```json
{
  "code": 200,
  "message": "回收站已清空",
  "data": null
}
```

---

### **8. 分享 (Share)**

#### **8.1. 创建分享链接**

`POST /shares`

**描述:** 为指定的文件或文件夹创建公开的分享链接。

**请求体:** `@RequestBody ShareCreateReq`

```json
{
  "file_id": 201,
  "folder_id": null,
  "is_encrypted": true,
  "expires_in_days": 7 //1 7 30 永久(0)
}
```

| 字段              | 类型    | 描述                             |
| :---------------- | :------ | :------------------------------- |
| `file_id`         | Long    | (可选) 要分享的文件ID。          |
| `folder_id`       | Long    | (可选) 要分享的文件夹ID。        |
| `is_encrypted`    | Boolean | 是否加密。`true`则会生成提取码。 |
| `expires_in_days` | Integer | 有效期天数。`0` 表示永久有效。   |

**备注:**  file_id 和 folder_id 必须且只能提供一个。

**响应:** `ShareCreateResp`

```json
{
  "code": 200,
  "message": "创建分享成功",
  "data": {
    "share_token": "a1b2c3d4e5",
    "share_password": "abcd"
  }
}
```

#### **8.2. 获取我的分享记录**

`GET /shares/me`

**描述:** 分页获取当前用户创建的所有分享记录。

**查询参数:**

| 参数   | 类型    | 是否必须 | 描述                |
| :----- | :------ | :------- | :------------------ |
| `page` | Integer | 否       | 页码，默认 1。      |
| `size` | Integer | 否       | 每页数量，默认 10。 |

**响应:** `ShareRecordPageResp`

```json
{
  "code": 200,
  "message": "获取我的分享列表成功",
  "data": {
    "total": 1,
    "current_page": 1,
    "page_size": 10,
    "records": [
      {
        "id": 15,
        "file_id": 201,
        "folder_id": null,
        "share_item_name": "项目报告.pdf",
        "share_token": "a1b2c3d4e5",
        "share_password": "abcd",
        "create_time": "2024-07-25T12:00:00Z",
        "expire_time": "2024-08-01T12:00:00Z"
      }
    ]
  }
}
```

#### **8.3. 取消分享**

`DELETE /shares/{share_id}`

**描述:** 取消一个分享，使其链接和提取码失效。

**路径参数:**

| 参数       | 类型 | 描述                                           |
| :--------- | :--- | :--------------------------------------------- |
| `share_id` | Long | 分享记录的ID (来自获取分享记录列表中的 `id`)。 |

**响应:**

```json
{
  "code": 200,
  "message": "分享已取消",
  "data": null
}
```

#### **8.4. 验证分享信息 (用于转存前确认)**

`POST /shares/verify`

**描述:** 在用户执行“转存到我的网盘”操作之前，通过 share_token 和可选的密码来验证分享链接，并获取分享项目的基本信息。这用于向用户展示一个确认界面（例如：“您要保存文件 ‘xxx.pdf’ (15MB) 吗？”）。

**请求体:** `@RequestBody ShareVerifyReq`

```json
{
  "share_token": "a1b2c3d4e5",
  "share_password": "abcd"
}
```

**响应:** `ShareInfoResp`

```json
{
  "code": 200,
  "message": "验证成功",
  "data": {
    "item_type": "file", // "file" 或 "folder" ItemType枚举类
    "item_name": "项目报告.pdf",
    "item_size": 15728640, // 如果是文件，为文件大小；如果是文件夹，为文件夹总大小
    "total_files": 1 // 如果是文件夹，为包含的文件总数
  }
}
```

> **前端流程:**
>
> 1. 用户输入分享链接，前端解析出 share_token 和 share_password。
> 2. 调用本接口。
> 3. 若成功，根据返回的 data 弹出确认框，并让用户选择要保存到的目标文件夹。

#### ！8.5. 转存分享内容到我的网盘 (异步)

`POST /shares/save`

**描述:** 将一个分享链接中的所有文件异步地“复制”到当前用户的指定文件夹中。由于是服务端操作，此接口会立即返回，并在后台处理。

**请求体:** `@RequestBody ShareSaveReq`

```json
{
  "share_token": "a1b2c3d4e5",
  "share_password": "abcd",
  "target_folder_id": 41
}
```

**响应 (快速返回):** `ShareSaveAsyncResp`

```json
{
  "code": 202,
  "message": "转存任务已创建，正在后台处理 3 个文件",
  "data": {
    "total_files_to_save": 3,
    "initial_file_data_list": [ // 返回一个初始文件对象列表！
      {
        "id": 210, // 在用户文件表中为“文件A”新创建的条目ID
        "name": "文件A.pdf", 
        "file_type": "document", //FileTypeEnum
        "status": "processing", //FileStatusEnum
        "create_time": "...",
        "update_time": "..."
      },
      {
        "id": 211, // 在用户文件表中为“文件B.mp3”新创建的条目ID
        "name": "文件B.mp3",
        "file_type": "music",
        "status": "processing",
        "create_time": "...",
        "update_time": "..."
      },
      {
        "id": 212, 
        "name": "文件C.mp3",
        "file_type": "music",
        "status": "processing",
        "create_time": "...",
        "update_time": "..."
      }
    ]
  }
}
```

---

### **9. 专注 (Focus)**

#### **9.1. 记录专注时长**

`POST /focus/records`

**描述:** 记录一次完成的专注会话。

**查询参数:**

| 参数            | 类型    | 是否必须 | 描述             |
| :-------------- | :------ | :------- | :--------------- |
| `focus_minutes` | Integer | 是       | 专注时长，整分钟 |

**响应:**

```json
{
  "code": 200,
  "message": "专注记录已保存，恭喜您获得新岛屿！",
  "data": "https://r2.liteisle.com/islands/island_05.png"
}
```

> **备注:** 如果本次专注没有获得新岛屿，`data` 字段将为 `null`，`message` 也会相应变化。

#### **9.2. 获取专注总次数**

`GET /focus/stats/total-count`

**描述:** 获取用户累计的专注总次数，用于仪表盘卡片展示。

**响应:**

```json
{
  "code": 200,
  "message": "获取成功",
  "data": 32
}
```

#### **9.3. 获取专注记录**

`GET /focus/stats/records`

**描述:** 分页获取用户的专注历史记录。

**查询参数:**

| 参数   | 类型    | 是否必须 | 描述                |
| :----- | :------ | :------- | :------------------ |
| `page` | Integer | 否       | 页码，默认 1。      |
| `size` | Integer | 否       | 每页数量，默认 10。 |

**响应:** `FocusStatsPageResp`

```json
{
  "code": 200,
  "message": "获取专注统计成功",
  "data": {
    "total": 32,
    "current_page": 1,
    "page_size": 10,
    "records": [
      {
        "id": 105, // 记录的ID
        "focus_minutes": 50,
        "create_time": "...."
      },
      {
        "id": 106, // 记录的ID
        "focus_minutes": 75,
        "create_time": "...."
      }
    ]
  }
}
```

#### **9.4. 获取专注日历数据**

`GET /focus/stats/calendar`

**描述:** 获取指定月份的专注日历数据，用于渲染类似 GitHub 的贡献图。

**查询参数:**

| 参数    | 类型    | 是否必须 | 描述              |
| :------ | :------ | :------- | :---------------- |
| `year`  | Integer | 是       | 年份，例如 2024。 |
| `month` | Integer | 是       | 月份，例如 7。    |

**响应:** `FocusCalendarResp`

```json
{
  "code": 200,
  "message": "获取专注日历成功",
  "data": {
    "year_month": "2024-7",
    "check_in_days": [1, 3, 5, 7, 24, 25],
    "total_check_in_count": 6,
    "total_focus_minutes": 480
  }
}
```

> **备注:** `total_focus_minutes` 是当月的总专注分钟数。

---

### **10. 岛屿 (Island)**

#### **10.1. 获取用户岛屿收集情况**

`GET /islands/me`

**描述:** 获取当前用户已经收集到的所有岛屿信息，用于仪表盘展示。

**响应:**

```json
{
  "code": 200,
  "message": "获取岛屿收集情况成功",
  "data": ["https://r2.liteisle.com/islands/island_01.png",
"https://r2.liteisle.com/islands/island_02.png"]
}
```

> **备注:**
>
> * `data` 数组中可能包含重复的岛屿。

### **11. 实用工具 (Utilities)**

此部分API提供跨模块的通用辅助功能，以增强核心用户体验。

#### **11.1. 划词翻译**

`POST /translate`

**描述:** 接收一段文本及其文档id，并返回其翻译结果。上下文的提供可以显著提升翻译的准确性和专业性。

**请求体:** `@RequestBody TranslateReq`

```json
{
  "text": "The framework uses a stack to manage states.",
  "target_lang": "zh-CN",
  "file_name": "401.pdf"
}
```

| 字段        | 类型   | 是否必须 | 描述                         |
| ----------- | ------ | -------- | ---------------------------- |
| text        | String | 是       | 用户划词选中的核心文本。     |
| target_lang | String | 否       | 目标语言代码 默认中文。      |
| file_name   | String | **是**   | 当前正在阅读或编辑的文件名。 |

**响应:** `TranslateResp`

```json
{
  "code": 200,
  "message": "翻译成功",
  "data": {
    "original_text": "Hello, this is a sentence to be translated.",
    "translated_text": "你好，这是一个需要被翻译的句子。"
  }
}
```

| 字段            | 类型   | 描述                           |
| --------------- | ------ | ------------------------------ |
| original_text   | String | 原始请求的文本，便于前端核对。 |
| translated_text | String | 翻译后的文本。                 |

---

# 