# Gymmate MVP

Gymmate 是一个 Android 原生 Kotlin + FastAPI 的极简健身训练 MVP。

当前仓库包含两部分：

1. `app/`：Android 客户端
2. `backend/`：FastAPI + SQLite 服务端

## 当前实现范围

- 动作库列表、搜索、详情
- 训练计划列表
- 历史记录列表
- 训练页 CameraX 预览
- MediaPipe Pose 接口层
- 骨架 overlay
- 本地规则引擎基础结构
- FastAPI + SQLite + FTS5 搜索
- DeepSeek 反馈代理占位

当前训练规则优先支持：

- 杠铃深蹲
- 哑铃侧平举
- 哑铃推肩
- 坐姿绳索划船
- 高位下拉
- 杠铃卧推
- 器械推胸
- 器械腿屈伸

规则设计说明见：

- `TRAINING_RULES_MVP.md`

## Android 技术栈

- Kotlin
- Jetpack Compose
- Navigation Compose
- Retrofit
- CameraX
- MediaPipe Pose Landmarker

## 后端技术栈

- FastAPI
- SQLite
- FTS5

## 运行后端

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

默认地址：

- `http://127.0.0.1:8000`

## 运行 Android

1. 使用 Android Studio 打开仓库根目录 `D:\dev\my-project\Gymmate`
2. 等待 Gradle 同步
3. 运行 `app`

## MediaPipe 模型文件

训练页已经接入 MediaPipe 接口层，默认读取：

- `app/src/main/assets/pose_landmarker_lite.task`

你需要手动放入官方 `Pose Landmarker` 的 `.task` 模型文件后，训练页才会真正开始推理。

如果不放模型文件：

- App 仍可打开训练页
- 可看到相机预览和训练流程
- 会显示“模型未加载，请放入 pose_landmarker_lite.task”

## DeepSeek 配置

如需启用真实 AI 反馈，在后端环境变量中配置：

- `DEEPSEEK_API_KEY`
- `DEEPSEEK_BASE_URL`（可选）
- `DEEPSEEK_MODEL`（可选）

未配置时会返回本地兜底反馈。

当前后端默认建议模型：

- `deepseek-v4-flash`

部署到 RackNerd 的完整方案见：

- `backend/DEPLOYMENT_RACKNERD.md`
