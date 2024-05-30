package com.ashin.client;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ashin.config.GptConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DifyClient {

    @Resource
    private GptConfig gptConfig;
    private Map<String, String> userToConversationId;

    public DifyClient() {
        userToConversationId = new HashMap<>();
    }


    /*
        *
    curl -X POST 'http://127.0.0.1:5001/v1/chat-messages' \
    --header 'Authorization: Bearer {api_key}' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "inputs": {},
        "query": "What are the specs of the iPhone 13 Pro Max?",
        "response_mode": "streaming",
        "conversation_id": "",
        "user": "abc-123",
        "files": [
          {
            "type": "image",
            "transfer_method": "remote_url"
            "url": "https://cloud.dify.ai/logo/logo-site.png"
          }
        ]
    }'
        *
        * */
    public String getFromDify(String user, String msg) {
        // 当前好友是否已有聊天历史
        boolean hasHis = false;
        String conversationId = "";
        if (userToConversationId.containsKey(user) && !"".equals(userToConversationId.get(user))) {
            hasHis = true;
            conversationId = userToConversationId.get(user);
        }

        String url = "http://127.0.0.1:5001/v1/chat-messages";
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("inputs", "");
        paramMap.put("query", msg);
        paramMap.put("response_mode", "streaming");
        paramMap.put("user", user);
        paramMap.put("conversation_id", conversationId);

        System.out.println(paramMap);
        System.out.println(JSONUtil.toJsonStr(paramMap));

        System.out.println("---*********************");
        System.out.println(String.format("Bearer %s", gptConfig.getDifyApikey()));
        System.out.println("---*********************");

        HttpResponse res = HttpRequest.post(url)
                .header(Header.AUTHORIZATION, String.format("Bearer %s", gptConfig.getDifyApikey()))
//                .header(Header.AUTHORIZATION, "Bearer app-")
                .header(Header.CONTENT_TYPE, "application/json")
                .body(JSONUtil.toJsonStr(paramMap))
                .execute();

        // 将双换行转成单个换行，使得拆分时不会出现空对象
        String body = res.body().replaceAll("\n\n", "\n");
        System.out.println("body----------------");
        System.out.println(body);

        List<String> list = Arrays.asList(body.split("\n"));
        String ret;

        try {
            // 是否有报错信息
            JSONObject jsonObject = JSONUtil.parseObj(list.get(list.size() - 1).replaceFirst("data:", ""));
            if ("error".equals(jsonObject.getStr("event"))) {
                ret = jsonObject.getStr("message");
                return ret;
            }

            // 是否取到完整回答
            jsonObject = JSONUtil.parseObj(list.get(list.size() - 2).replaceFirst("data:", ""));
            saveConversation(!hasHis, user, jsonObject.getStr("conversation_id"));
            // openai接口
            if ("agent_thought".equals(jsonObject.getStr("event"))) {
                ret = jsonObject.getStr("thought");
                return ret;
            }
            // 星火大模型返回
            else if("agent_message".equals(jsonObject.getStr("event"))) {
                ret = jsonObject.getStr("answer");
                return ret;
            }
            else if("message".equals(jsonObject.getStr("event"))) {
                ret = jsonObject.getStr("answer");
                return ret;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // 解析错误
        return body;
    }

    public void reset(String user) {
            this.userToConversationId.put(user, "");
    }

    private void saveConversation(boolean flag, String user, String conversationId) {
        if (flag) {
            userToConversationId.put(user, conversationId);
        }
    }
}
