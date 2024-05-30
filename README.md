# ChatGPT-YourChatRobot
## 简介

这是一个**开箱即用**的**非官方**的聊天机器人, 通过在部署本地[dify](https://github.com/langgenius/dify)（或[cloud.dify](https://cloud.dify.ai/apps)），连接dify提供的API，实现微信对接大语言模型驱动的生成式AI应用

微信机器人实现基于[TheoKanning/openai-java](https://github.com/TheoKanning/openai-java)和[wxmbaci/itchat4j-uos](https://github.com/wxmbaci/itchat4j-uos).

## 使用
1. 本地源码启动dify（若使用 [cloud.dify](https://cloud.dify.ai/apps) 或docker一键启动则跳过此步骤）

   1.1 clone Dify 代码（本项目已提供）

   ```bash
   git clone https://github.com/langgenius/dify.git
   ```

   1.2 使用docker部署PostgresSQL / Redis / Weaviate

   ```bash
   cd docker
   docker compose -f docker-compose.middleware.yaml up -d
   ```

   1.3 服务端部署

   **安装基础环境**

   ```bash
   # 创建名为 dify 的 Python 3.10 环境
   conda create --name dify python=3.10
   # 切换至 dify Python 环境
   conda activate dify
   ```

   **启动步骤**

   ```bash
   cd api
   cp .env.example .env
   # 生成随机密钥
   openssl rand -base64 42
   sed -i 's/SECRET_KEY=.*/SECRET_KEY=<your_value>/' .env
   # 安装依赖包
   pip install -r requirements.txt
   # 执行数据库迁移，将数据库结构迁移至最新版本
   flask db upgrade
   # 启动API服务
   flask run --host 0.0.0.0 --port=5001 --debug
   ```

   ```bash
   # 启动Worker服务
   # Linux/MacOS启动
   celery -A app.celery worker -P gevent -c 1 -Q dataset,generation,mail --loglevel INFO
   # Windows启动
   celery -A app.celery worker -P solo --without-gossip --without-mingle -Q dataset,generation,mail --loglevel INFO
   ```

   1.4 前端页面docker部署

   ```bash
   docker run -it -p 3000:3000 -e EDITION=SELF_HOSTED -e CONSOLE_URL=http://127.0.0.1:5001 -e APP_URL=http://127.0.0.1:5001 langgenius/dify-web:latest
   ```

   即可访问dify页面 http://127.0.0.1:3000

2. 创建用户并在dify中新建自己的项目（本项目已有一个demo，如不需要，删除volume文件夹重新启动，账号：lijun1159749673@qq.com，密码：abc123..）

   在设置>模型供应商中配置需要使用的模型供应商的API-KEY

3. 进入一个应用，在概览>后端服务API>API密钥中得到密钥，将密钥配置在微信客户端项目中的application.yml里

4. 启动MyChatGptApplication项目，扫码登录微信，此时所登录的微信即可作为你的Agent助手

