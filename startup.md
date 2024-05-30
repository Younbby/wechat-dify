## 本地源码启动
1. docker启动中间件
    ```shell
    docker-compose -f .\docker-compose.middleware.yaml -p dify up -d
    ```
   
2. 启动api
   ```shell
   # 执行数据库迁移
   # 将数据库结构迁移至最新版本。
   flask db upgrade
   
   # 启动 API 服务
   flask run --host 0.0.0.0 --port=5001 --debug
   ```
   
3. 启动 Worker 服务

   用于消费异步队列任务，如数据集文件导入、更新数据集文档等异步操作。
   ```shell
   celery -A app.celery worker -P solo --without-gossip --without-mingle -Q dataset,generation,mail --loglevel INFO
   ```
   
4. docker启动前端
   ```shell
   docker run -it -p 3000:3000 -e EDITION=SELF_HOSTED -e CONSOLE_URL=http://127.0.0.1:5001 -e APP_URL=http://127.0.0.1:5001 langgenius/dify-web:latest
   ```
   
本地访问 http://127.0.0.1:3000