# API 文档与交互流程

## 一、设计说明

### 设计目标
- 支持大文件上传，突破单次请求大小限制
- 支持分片并发、乱序、重试
- 不依赖客户端请求顺序
- 明确上传完成边界

### 核心约束
- 单次 HTTP 请求最大安全处理能力
- 网络不可靠，请求可能乱序、重复、失败
- 上传完成状态必须由服务端判定

## 二、整体交互流程（文字说明）
1. 客户端调用 **上传协商接口**，获取上传策略  
2. 客户端按策略并发上传分片数据  
3. 客户端调用 **完成确认接口** 声明上传结束  
4. 服务端校验分片完整性  
5. 校验通过后，服务端异步合并文件并触发后续处理  

## 三、API 定义

### 1. 上传协商接口

**请求信息**
- URL：`POST /upload/init`
- Content-Type：`application/json`

**请求参数**

| 参数名 | 类型 | 是否必填 | 说明 |
|------|------|----------|------|
| fileName | string | 否 | 文件名 |
| fileSize | number | 是 | 文件大小（字节） |

**请求示例**
```json
{
  "fileName": "example.zip",
  "fileSize": 209715200
}
```

**响应参数**

| 参数名 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 本次上传唯一标识 |
| totalChunks | number | 总分块数 |

**响应示例**
```json
{
  "uploadId": "u-123456",
  "totalChunks": 4
}
```

---

### 2. 分片上传接口

**请求信息**
- URL：`POST /upload/chunk`
- Content-Type：`multipart/form-data`

**请求参数**

| 参数名 | 类型 | 是否必填 | 说明 |
|------|------|----------|------|
| uploadId | string | 是 | 上传唯一标识 |
| chunkIndex | number | 是 | 分片序号（从 0 开始） |
| chunkSize | number | 是 | 分片大小（字节） |
| file | binary | 是 | 分片数据 |

**请求说明**
- `chunkIndex` 必须在 `[0, totalChunks - 1]` 范围内  
- 同一分片允许重复上传（幂等）  

**响应参数**

| 参数名 | 类型 | 说明 |
|------|------|------|
| status | string | 上传结果 |

**响应示例**
```json
{
  "status": "OK"
}
```

---

### 3. 上传完成确认接口

**请求信息**
- URL：`POST /upload/complete`
- Content-Type：`application/json`

**请求参数**

| 参数名 | 类型 | 是否必填 | 说明 |
|------|------|----------|------|
| uploadId | string | 是 | 上传唯一标识 |

**请求示例**
```json
{
  "uploadId": "u-123456"
}
```

**响应示例（完成）**
```json
{
  "status": "COMPLETED"
}
```

**响应示例（未完成）**
```json
{
  "status": "INCOMPLETE",
  "missingChunks": [2]
}
```
