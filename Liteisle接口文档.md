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
    "picture": "https://r2.liteisle.com/pictures/default.png",
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
    "picture": "https://r2.liteisle.com/pictures/12345.jpg",
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

#### 3.0. 云盘页面全局搜索（递归所有文件文件夹）

>  **后端实现说明**: 此接口的数据源为Elasticsearch。当用户在MySQL中创建、更新或删除文件/文档时，会通过RabbitMQ异步通知一个同步服务，将最新的数据写入ES。因此，搜索结果是“准实时”的。

`GET /search`

**描述:** 云盘页面的搜索框，输入返回所有匹配文件！

**查询参数:**

| 参数      | 类型   | 是否必须 | 描述     |
| :-------- | :----- | :------- | :------- |
| `content` | String | 是       | 匹配字段 |

**响应:** `SearchContentResp`

```json
{
  "code": 200,
  "message": "搜索成功",
  "data": [ // 直接返回一个扁平的数组，每个对象都是一个搜索结果
    {
      "item_type": "file", // 明确类型是文件还是文件夹
      "id": 201,
      "name": "项目报告.pdf",
      "path": "/文档/工作/项目报告.pdf", // ✨ 后端直接生成好的可读路径
      "file_type": "document",
      "file_size": 2097152,
      "status": "available",
      "update_time": "2024-01-02T09:00:00Z"
    },
    {
      "item_type": "folder",
      "id": 105,
      "name": "项目报告素材",
      "path": "/分享/项目报告素材", // ✨ 文件夹也有路径
      "sub_item_count": 8,
      "update_time": "2024-01-02T10:30:00Z"
    }
    // ...更多结果
  ]
}
```

#### **3.1. 获取指定文件夹内容**

`GET /folders/{folder_id}`

**描述:** 获取指定文件夹下的所有文件夹和文件列表。这是云盘主界面的核心接口。

**路径参数:**

| 参数        | 类型 | 描述                                   |
| :---------- | :--- | :------------------------------------- |
| `folder_id` | Long | 文件夹ID。**特别地，`0` 代表根目录**。 |

**查询参数:**

| 参数      | 类型   | 是否必须 | 描述                                                         |
| :-------- | :----- | :------- | :----------------------------------------------------------- |
| `sort_by` | String | 否       | 排序字段，可选值: `name`, `file_size`, `create_time`, `update_time`, `sorted_order` (默认) |

**响应:** `FolderContentResp`

```json
{
  "code": 200,
  "message": "获取列表成功",
  "data": {
      "breadcrumb": [
  	{ 		
        "id": 0, 
        "name": "云盘" 
    },
  	{ 
        "id": 1, 
        "name": "文档" 
    },
  	{ 
        "id": 101, 
        "name": "工作" 
    }
	],
    "folders": [
      {
        "id": 101,
        "name": "我的文档",
        "folder_type": "notebook",
        "sub_item_count": 5,
        "sorted_order": 1704186000000.123124124124,
        "create_time": "2024-01-02T09:00:00Z",
        "update_time": "2024-01-02T10:30:00Z"
      }
    ],
    "files": [
      {
        "id": 201,
        "name": "项目报告.pdf",
        "file_type": "document",
        "file_size": 2097152,
        "status": "available",
        "sorted_order": 1704186000321000.123124124124,
        "create_time": "2024-01-02T09:00:00Z",
        "update_time": "2024-01-02T09:00:00Z"
      },
      {
        "id": 202,
        "name": "临时录音.mp3",
        "file_type": "music",
        "file_size": 0,
        "status": "processing",
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
> * `folder_type`: 枚举 `system`(系统), `playlist`(歌单), `notebook`(笔记本)。
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
  "folder_type": "playlist" // 明确指定类型
}
```

> **注意:** `parent_id` 不能为 `0`，根目录下不允许用户直接创建文件夹，系统文件夹由系统管理。

**响应:** `FolderInfoResp` (返回新创建的文件夹信息)

```json
{
  "code": 200,
  "message": "文件夹创建成功",
  "data": {
    "id": 102,
    "name": "新建文件夹",
    "folder_type": "share",
    "parent_id": 101,
    "create_time": "2024-07-25T10:00:00Z",
    "update_time": "2024-07-25T10:00:00Z"
  }
}
```

#### **3.3. 重命名文件或文件夹**

##### **3.3.1 重命名文件**

`PUT /files/{file_id}/rename`

**描述:** 重命名一个文件。

**路径参数:**

| 参数    | 类型 | 描述                 |
| ------- | ---- | -------------------- |
| file_id | Long | 要重命名的文件的ID。 |

**请求体:** `@RequestBody ItemRenameReq`

```json
{
  "new_name": "新的项目报告.pdf"
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

##### **3.3.2 重命名文件夹**

`PUT /folders/{folder_id}/rename`

**描述:** 重命名一个文件夹。

**路径参数:**

| 参数      | 类型 | 描述                   |
| --------- | ---- | ---------------------- |
| folder_id | Long | 要重命名的文件夹的ID。 |

**请求体:** `@RequestBody ItemRenameReq`

```json
{
  "new_name": "新的项目文件夹"
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

#### **3.4. 移动文件或文件夹**

`PUT /items/move`

**描述:** 将一个或多个文件/文件夹移动到新的目标文件夹。（前后端双重校验防止文件夹嵌套）

**请求体:** `@RequestBody ItemsMoveReq`

```json
{
  "file_ids": [201],
  "folder_ids": [102],
  "target_folder_id": 3
}
```

> **备注:** 请求体中明确区分 file_ids 和 folder_ids 数组，两者至少提供一个。target_folder_id 为 0 代表移动到根目录。

**响应:**

```json
{
  "code": 200,
  "message": "移动成功",
  "data": null
}
```

#### **3.5. 删除文件或文件夹 (移入回收站)**

`DELETE /items`

**描述:** 将一个或多个文件/文件夹移入回收站（软删除）。

**请求体:** `@RequestBody ItemsDeleteReq`

```json
{
  "file_ids": [201],
  "folder_ids": [102]
}
```

> **备注:** 请求体中明确区分 file_ids 和 folder_ids 数组，两者至少提供一个。

**响应:**

```json
{
  "code": 200,
  "message": "已将 2 个项目移入回收站",
  "data": null
}
```

#### **3.6. 获取项目详情 (用于右键菜单)**

为了明确操作对象，此功能拆分为两个独立的API。

##### **3.6.1 获取文件详情**

`GET /files/{file_id}/detail`

**描述:** 获取单个文件的详细信息。

**路径参数:**

| 参数    | 类型 | 描述       |
| ------- | ---- | ---------- |
| file_id | Long | 文件的ID。 |

**响应:** `ItemDetailResp`

```json
{
  "code": 200,
  "message": "获取详情成功",
  "data": {
    "id": 201,
    "name": "项目报告.pdf",
    "item_type": "file", 
    "size": 2097152,
    "path": "/我的文档/项目报告.pdf",
    "create_time": "2024-01-02T09:00:00Z",
    "update_time": "2024-01-02T09:00:00Z"
  }
}
```

##### **3.6.2 获取文件夹详情**

`GET /folders/{folder_id}/detail`

**描述:** 获取单个文件夹的详细信息。

**路径参数:**

| 参数      | 类型 | 描述         |
| --------- | ---- | ------------ |
| folder_id | Long | 文件夹的ID。 |

**响应:** `ItemDetailResp`

```json
{
  "code": 200,
  "message": "获取详情成功",
  "data": {
    "id": 101,
    "name": "我的文档",
    "item_type": "folder", 
    "size": 0,
    "path": "/我的文档",
    "create_time": "2024-01-02T09:00:00Z",
    "update_time": "2024-01-02T09:00:00Z"
  }
}
```

#### **3.7. 获取所有文件夹层级 (用于移动对话框)**

`GET /folders/hierarchy`

**描述:** 获取用户所有文件夹的树状层级结构，用于“移动到...”功能的对话框。

**响应:** `FolderHierarchyResp`

```json
{
  "code": 200,
  "message": "获取文件夹层级成功",
  "data": [
    {
      "id": 1,
      "folder_name": "音乐",
      "folder_type": "system",
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
      "folder_type": "notebook",
      "parent_id": 0
    }
  ]
}
```

> **备注:** `parent_id` 为 `0` 代表根目录下的文件夹。

#### 3.8. 复制

`POST /items/copy`

**描述:** 将一个或多个文件/文件夹复制到新的目标文件夹。这会在数据库中创建新的条目，但对于文件，会复用底层的物理存储（增加引用计数）。

**请求体:** `@RequestBody ItemsCopyReq`

```json
{
  "file_ids": [201],
  "folder_ids": [102],
  "target_folder_id": 3
}
```

> **备注:** 请求体中明确区分 file_ids 和 folder_ids 数组，两者至少提供一个。target_folder_id 为 0 代表移动到根目录。

**响应:**

```json
{
  "code": 200,
  "message": "复制成功",
  "data": null 
}
```

---

### **4. 文件上传与下载**

#### **4.1. 上传文件 (异步处理)**

`POST /upload`

**描述:** 接收文件并立即返回，将耗时的文件处理（如元数据提取）交由后台异步完成。

**请求体:** `multipart/form-data`

| 字段名 | 类型 | 是否必须 | 描述           |
| :----- | :--- | :------- | :------------- |
| `file` | File | 是       | 要上传的文件。 |

请求参数

| 字段名      | 类型 | 是否必须 | 描述             |
| :---------- | :--- | :------- | :--------------- |
| `folder_id` | Long | 是       | 要上传的文件夹。 |

**响应 (快速返回):** `FileUploadAsyncResp`

```json
{
  "code": 202,
  "message": "文件已接收，正在后台处理",
  "data": {
    "log_id": 123, // ✨ 新增
    "file_id": 203,
    "status": "processing",
    "initial_file_data": {
        "id": 203,
        "name": "晴天.mp3",
        "file_type": "music",
        "status": "processing",
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

#### **4.2. 获取下载链接**

为了明确操作对象，此功能拆分为两个独立的API。

##### 4.2.1 获取下载清单 (Manifest)

`POST /items/manifest`

**描述:** 这是所有**下载到本地**操作的**唯一入口**。无论是下载单个文件、多选的多个文件，还是一个“扁平文件夹”（如播放列表），都通过此接口获取一份包含所有待下载文件信息的扁平清单。

**请求体:** `@RequestBody ItemsSelectionReq`

```json
// 下载单个文件
{ "file_ids": [201] }

// 下载多个文件
{ "file_ids": [201, 202, 203] }

// 下载一个“扁平文件夹”
{ "folder_ids": [101] } // 后端会自动展开这个文件夹下的所有文件

// 混合下载
{ "file_ids": [204], "folder_ids": [101] }
```

| 字段       | 类型        | 是否必须 | 描述                                         |
| ---------- | ----------- | -------- | -------------------------------------------- |
| file_ids   | Array<Long> | 否       | 要包含在清单中的独立文件的 ID 列表。         |
| folder_ids | Array<Long> | 否       | 要递归展开并包含在清单中的文件夹的 ID 列表。 |

> **备注:** file_ids 和 folder_ids 至少需要提供一个。

**响应:** `ManifestResp`

```json{
  "code": 200,
  "message": "获取下载清单成功",
  "data": {
    "total_size": 31457280,
    "total_files": 5,
    "items": [ // 永远是一个扁平的文件列表
      {
        "type": "directory",
        "relative_path": "我的项目" 
      },
      {
        "type": "directory",
        "relative_path": "我的项目/文档" // 即使是文件夹，也展开为带路径的文件
      },
      {
        "type": "file",
        "relative_path": "我的项目/文档/会议记录.docx",
        "file_id": 202,
        "size": 20480
      },
      {
        "type": "file",
        "relative_path": "我的项目/封面图.jpg",
        "file_id": 201,
        "size": 512000
      },
      {
        "type": "file",
        "relative_path": "独立文件A.pdf",
        "file_id": 203,
        "size": 1048576
      },
       {
        "type": "file",
        "relative_path": "独立文件B.mp4",
        "file_id": 204,
        "size": 29875100
      }
    ]
  }
}
```

**响应体 data 字段详解**

| 字段        | 类型          | 描述                                                         |
| ----------- | ------------- | ------------------------------------------------------------ |
| total_size  | Long          | 所有待下载文件的总大小（单位：字节）。用于客户端计算总体下载进度。 |
| total_files | Integer       | 所有待下载文件的总数量。用于客户端显示文件计数进度（如 "3/5"）。 |
| items       | Array<Object> | 一个扁平化的项目数组，包含了所有需要创建的目录和需要下载的文件。客户端应按此顺序处理。 |

**items 数组中每个对象的结构**

| 字段          | 类型   | 描述                                                         |
| ------------- | ------ | ------------------------------------------------------------ |
| type          | String | 项目类型。枚举值：directory（目录）或 file（文件）。         |
| relative_path | String | **核心字段。** 项目相对于下载根目录的完整路径。客户端将使用此路径在本地创建对应的目录或文件。<br> - 对于被选中的文件夹，路径以其自身名称开始。<br> - 对于被选中的独立文件，路径就是其文件名。 |
| file_id       | Long   | **仅当 type 为 file 时存在。** 文件的唯一标识符。客户端将使用此ID调用 GET /download/file/{file_id} 来获取该文件的具体下载链接。 |
| size          | Long   | **仅当 type 为 file 时存在。** 文件的大小（单位：字节）。用于计算单个文件的下载进度和校验。 |

##### **4.2.2 获取单个文件下载链接**

`GET /download/file/{file_id}`

**描述:** 在获取 manifest 清单后，客户端（Electron）循环调用此接口，为清单中的每一个 file_id 获取实际的、带有时效性签名的下载 URL。

**路径参数:**

| 参数    | 类型 | 描述       |
| ------- | ---- | ---------- |
| file_id | Long | 文件的ID。 |

**响应:** `ItemDownloadResp`

```json
{
  "code": 200,
  "message": "获取下载地址成功",
  "data": [
    {
      "file_id": 201,
      "file_name": "项目报告.pdf",
      "file_size": 2097152,
      "download_url": "https://r2.liteisle.com/downloads/a1b2c3d4e5?token=..."
    }
  ]
}
```

---

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
| `status` | String  | 否       | `processing`: 进行中, `completed`: 已完成(含成功和失败)。不传则获取所有。 |
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
        "transfer_type": "upload",
        "status": "success",
        "create_time": "2024-07-25T10:00:00Z"
      },
      {
        "log_id": 2,
        "item_name": "晴天.mp3",
        "item_size": 5242880,
        "transfer_type": "upload",
        "status": "failed",
        "create_time": "2024-07-25T09:55:00Z"
      }
    ]
  }
}
```

#### **5.3. 删除单条传输记录**

`DELETE /transfers/{log_id}`

**描述:** 从传输列表中删除一条记录。

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

#### **5.4. 清空已完成的传输记录**

`DELETE /transfers/completed`

**描述:** 清空所有已完成（成功或失败）的传输记录。**此操作仅删除日志，不影响实际文件**。

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

#### 5.5. 取消上传任务

`POST /transfers/upload/{log_id}/cancel`

**描述:** 取消一个正在进行的上传任务。此操作会中断文件传输，并清理服务器上已接收的该文件的临时数据。

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

#### 5.6. **创建下载传输记录**

`POST /transfers/download`

**描述:** 在客户端准备开始下载前调用，用于在后端创建传输日志记录。

**请求体:** `@RequestBody DownloadRegisterReq`

```json
{
  "items": [
    { 
        "file_id": 201, 
        "item_name": "报告.pdf", 
        "item_size": 1024 
    },
    { 
        "folder_id": 102, 
        "item_name": "项目文件夹", 
        "item_size": 204800 
    }
  ]
}
```

**响应:** `DownloadRegisterResp`

```JSON
{
  "code": 200, "message": "下载任务已登记",
  "data": [
    { 
        "item_name": "报告.pdf", 
        "log_id": 124 
    },
    { 
        "item_name": "项目文件夹", 
        "log_id": 125 
    }
  ]
}
```

#### 5.7. 更新任务状态

`PUT /transfers/{log_id}/status`

**描述:** 由客户端在下载任务结束（成功、失败、取消）后调用，以更新其在后端的日志状态。

**路径参数:**

| 参数     | 类型 | 描述             |
| -------- | ---- | ---------------- |
| `log_id` | Long | 要更新的日志ID。 |

**请求体:** `@RequestBody TransferStatusUpdateReq`

```json
{
  "status": "success",
  "error_message": null,
  "duration_ms": 15000
}
```

**响应:**

```JSON
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

**描述:** 获取音乐页面所需的所有数据，包括所有播放列表（Playlist）和所有音乐文件。此接口用于页面初始化，前端可将数据存入状态管理库（Store）。

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
        "folder_type": "playlist",
        "sorted_order": 1704186000000,
        "music_count": 2
      }
    ],
    "files": [
      {
        "id": 205,
        "folder_id": 101,
        "name": "晴天.mp3",
        "file_type": "music",
        "sorted_order": 1704186100000,
        "artist": "周杰伦",
        "album": "叶惠美",
        "duration": 269,
        "cover_art_url": "https://r2.liteisle.com/covers/yehuimei.jpg"
      },
      {
        "id": 206,
        "folder_id": 101,
        "name": "稻香.mp3",
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

**描述:** 获取文档页面所需的所有数据，包括所有笔记本（Notebook）和所有文档文件。

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
    "notebooks": [
      {
        "id": 301,
        "name": "会议纪要",
        "folder_type": "notebook",
        "sorted_order": 1704187000000.83278441,
        "document_count": 1
      }
    ],
    "files": [
      {
        "id": 401,
        "folder_id": 301,
        "name": "3-18周会.md",
        "file_type": "document",
        "sorted_order": 1704187100000.8712367812412
      },
      {
        "id": 402,
        "folder_id": 0,
        "name": "产品需求文档.pdf",
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
    "storage_url": "https://r2.liteisle.com/md/123ouashd.md",
    "content": "# 周会纪要\n\n- 讨论了Q2季度目标。\n- 分配了新功能开发任务。",
    "version": 5 // 版本号
  }
}
```

##### **6.2.4. 保存 MD 文档**

`PUT /documents/md/{file_id}`

**描述:** 更新并保存在线编辑器中修改后的 Markdown 文档内容。

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

**描述:** 更新并保存在线编辑器中修改后的 Markdown 文档内容。

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

#### **6.3. 排序音乐与文档接口（file）**

`PUT /files/{file_id}/set-order`

**描述:** 在音乐页面拖拽歌曲，在文档页面拖拽文档。前端传递新位置前面的id（new_position_before_id）和后面的id（new_position_after_id）。

**请求参数**：

| 参数      | 类型 | 描述                         |
| :-------- | :--- | :--------------------------- |
| `file_id` | Long | 要移动的 音乐/文档文件的ID。 |

**请求体:** `@RequestBody SetOrderReq`

```json
{
  "new_position_before_id": 3,
  "new_position_after_id": 6
}
```

**响应:**

```json
{
  "code": 200,
  "message": "排序已更新",
  "data": null
}
```

#### **6.4. 排序音乐与文档接口（folder）**

`PUT /folders/{folder_id}/set-order`

**请求参数**：

| 参数        | 类型 | 描述                         |
| :---------- | :--- | :--------------------------- |
| `folder_id` | Long | 要移动的歌单、文档分类的ID。 |

**描述:** 在音乐页面拖拽歌单，在文档页面拖拽文档分类。前端传递新位置前面的id（new_position_before_id）和后面的id（new_position_after_id）。

**请求体:** `@RequestBody SetOrderReq`

```json
{
  "new_position_before_id": 3,
  "new_position_after_id": 6
}
```

**响应:**

```json
{
  "code": 200,
  "message": "排序已更新",
  "data": null
}
```

---

### **7. 回收站 (Recycle Bin)**

#### **7.1. 获取回收站内容**

`GET /recycle-bin`

**描述:** 获取当前用户回收站中的所有文件和文件夹。

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
        "original_type": "normal",
        "sub_item_count": 3,
        "delete_time": "2024-07-20T11:00:00Z",
        "expire_time": "2024-08-19T11:00:00Z"
      }
    ],
    "files": [
      {
        "original_id": 208,
        "original_name": "草稿.txt",
        "original_type": "document",
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

**描述:** 将回收站中的一个或多个项目还原到其原始位置。

**请求体:** `@RequestBody RecycleBinRestoreReq`

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

`DELETE /recycle-bin`

**描述:** 从回收站中永久删除一个或多个项目。此操作不可恢复。

**请求体:** `@RequestBody RecycleBinPurgeReq`

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

**描述:** 永久删除回收站中的所有项目。此操作不可恢复。

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
  "is_encrypted": true,
  "expires_in_days": 7
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
    "item_type": "file", // "file" 或 "folder"
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

#### **8.5. 转存分享内容到我的网盘 (异步)**

`POST /shares/save`

**描述:** 将一个分享链接中的所有文件异步地“复制”到当前用户的指定文件夹中。由于是服务端操作，此接口会立即返回，并在后台处理。（写死用户的分享文件夹，后端会填充）

**请求体:** `@RequestBody ShareSaveReq`

```json
{
  "share_token": "a1b2c3d4e5",
  "share_password": "abcd"
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
        "file_type": "document",
        "status": "processing",
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
        "id": 212, // 在用户文件表中为“文件C.jpg”新创建的条目ID
        "name": "文件C.jpg",
        "file_type": "image",
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

**请求体:** `@RequestBody FocusRecordReq`

```json
{
  "focus_minutes": 25
}
```

**响应:** `FocusRecordResp`

```json
{
  "code": 200,
  "message": "专注记录已保存，恭喜您获得新岛屿！",
  "data": {
    "island_id": 5,
    "image_url": "https://r2.liteisle.com/islands/island_05.png"
  }
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
        "session_id": 105, // 记录的ID
        "completed_at": "2024-07-25T14:30:00Z", // 精确的完成时间
        "focus_minutes": 50
      },
      {
        "session_id": 106, // 记录的ID
        "completed_at": "2024-07-25T14:30:00Z", // 精确的完成时间
        "focus_minutes": 75
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
    "year_month": "2024-07",
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

**响应:** `IslandCollectionResp`

```json
{
  "code": 200,
  "message": "获取岛屿收集情况成功",
  "data": {
    "total_island_types": 20,
    "obtained_island_types": 4,
    "obtained_islands_count": 5,
    "islands_info": [
      {
        "island_id": 1,
        "image_url": "https://r2.liteisle.com/islands/island_01.png"
      },
      {
        "island_id": 2,
        "image_url": "https://r2.liteisle.com/islands/island_02.png"
      },
      {
        "island_id": 1,
        "image_url": "https://r2.liteisle.com/islands/island_01.png"
      },
      {
        "island_id": 5,
        "image_url": "https://r2.liteisle.com/islands/island_05.png"
      },
      {
        "island_id": 8,
        "image_url": "https://r2.liteisle.com/islands/island_08.png"
      }
    ]
  }
}
```

> **备注:**
>
> * `islands_info` 数组中可能包含重复的岛屿。
> 
> 
# sql
```sql
-- =================================================================
-- 用户核心模块 (User Core Module)
-- =================================================================

-- 1. 用户表
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `picture_url` varchar(512) DEFAULT NULL,
  `storage_quota` bigint DEFAULT 5368709120 COMMENT '默认5GB',
  `storage_used` bigint DEFAULT 0,
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户与基本信息';

-- 2. 用户专注记录表
CREATE TABLE `user_focus_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `focus_minutes` int DEFAULT 0,
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_focus_time` (`user_id`, `create_time` DESC), -- 添加一个普通索引以便快速查询
  CONSTRAINT `fk_focus_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户单次专注会话记录';

-- 3. 用户岛屿收集表
CREATE TABLE `user_islands` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `island_code` varchar(50) NOT NULL COMMENT '岛屿的唯一代码，与后端配置对应',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_islands_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记录用户已解锁的岛屿';


-- =================================================================
-- 文件系统模块 (File System Module)
-- =================================================================

-- 4. 物理文件存储表 (秒传核心)
CREATE TABLE `storages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_hash` varchar(64) NOT NULL COMMENT '文件内容的哈希值 (e.g., SHA-256)',
  `file_size` bigint NOT NULL,
  `mime_type` varchar(100) NOT NULL,
  `storage_path` varchar(512) NOT NULL COMMENT '在对象存储(R2)中的路径或Key',
  `reference_count` int DEFAULT 1 COMMENT '引用计数，为0时可由后台任务清理',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储唯一文件实体，避免重复上传';

-- 5. 文件夹表
CREATE TABLE `folders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT '父文件夹ID, NULL表示根目录',
  `name` varchar(255) NOT NULL,
  `folder_type` enum('system', 'playlist', 'notebook','upload','share') NOT NULL COMMENT 'system: 音乐/文档/上传/分享, playlist: 用户创建的歌单, notebook: 文档分类,upload:系统文件夹-》“上传”里的文件夹,share:系统文件夹-》“分享”里的文件夹',
  `sorted_order` double DEFAULT 0 COMMENT '用于用户自定义排序',
  `delete_time` timestamp NULL DEFAULT NULL COMMENT '软删除标记，用于回收站功能',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_parent` (`user_id`, `parent_id`),
  CONSTRAINT `fk_folders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑文件夹，统一管理歌单、文档分类等';


-- 6. 文件元数据表 (最终版，支持异步处理)
CREATE TABLE `files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `folder_id` bigint NOT NULL,
  `storage_id` bigint DEFAULT NULL COMMENT '正式存储ID，在处理完成前可为空',
  `name` varchar(255) NOT NULL,
  `extension` varchar(20) DEFAULT NULL,
  `file_type` enum('music', 'document') NOT NULL,
  `status` enum('processing', 'available', 'failed') NOT NULL DEFAULT 'processing' COMMENT '文件状态',
  `temp_storage_path` varchar(512) DEFAULT NULL COMMENT '文件在云存储中的临时路径',
  `error_message` varchar(512) DEFAULT NULL,
  `sorted_order` double DEFAULT 0 COMMENT '用于用户自定义排序',
  `delete_time` timestamp NULL DEFAULT NULL,
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  -- 元数据字段
  `artist` varchar(255) DEFAULT NULL,
  `album` varchar(255) DEFAULT NULL,
  `duration` int DEFAULT NULL COMMENT '秒',
  `cover_art_url` varchar(512) DEFAULT NULL,
  `content` LONGTEXT,
  `version` bigint DEFAULT 1,
  
  PRIMARY KEY (`id`),
  KEY `idx_user_folder_status` (`user_id`, `folder_id`, `status`),
  CONSTRAINT `fk_files_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_files_folder` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_files_storage` FOREIGN KEY (`storage_id`) REFERENCES `storages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据及处理状态';


-- =================================================================
-- 分享与传输模块 (Sharing & Transfer Module)
-- =================================================================

-- 7. 分享表
CREATE TABLE `share_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `share_token` varchar(64) NOT NULL,
  `share_password` varchar(255) DEFAULT NULL,
  `file_id` bigint DEFAULT NULL,
  `folder_id` bigint DEFAULT NULL,
  `expire_time` timestamp NULL DEFAULT NULL,
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  CONSTRAINT `fk_share_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_share_target` CHECK ((`file_id` IS NOT NULL AND `folder_id` IS NULL) OR (`file_id` IS NULL AND `folder_id` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理公开分享链接';


-- 8. 传输日志表
CREATE TABLE `transfer_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `transfer_type` enum('upload', 'download') NOT NULL,
  `file_id` bigint DEFAULT NULL,
  `folder_id` bigint DEFAULT NULL,
  `item_name` varchar(255) NOT NULL,
  `item_size` bigint NOT NULL COMMENT '字节',
  `status` enum('processing', 'success', 'failed', 'canceled') NOT NULL DEFAULT 'processing' COMMENT '传输行为状态: 处理中, 成功, 失败, 已取消',
  `error_message` varchar(512) DEFAULT NULL,
  `duration_ms` int DEFAULT NULL,
  `client_ip` varchar(45) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_status_time` (`user_id`, `status`, `create_time` DESC),
  CONSTRAINT `fk_tlog_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tlog_file` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tlog_folder` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传下载行为日志';
```