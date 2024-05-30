package com.ashin.handler;

import com.ashin.client.DifyClient;
import com.ashin.config.KeywordConfig;
import com.ashin.constant.ChatType;
import com.ashin.entity.bo.ChatBO;
import com.ashin.service.InteractService;
import com.ashin.util.BotUtil;
import cn.zhouyafeng.itchat4j.beans.BaseMsg;
import cn.zhouyafeng.itchat4j.core.Core;
import cn.zhouyafeng.itchat4j.face.IMsgHandlerFace;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 微信消息处理程序
 *
 * @author ashinnotfound
 * @date 2023/03/19
 */
public class WechatMessageHandler implements IMsgHandlerFace {
    private final InteractService interactService;
    private final KeywordConfig keywordConfig;
    private final BotUtil botUtil;
    private DifyClient difyClient;

    public WechatMessageHandler(InteractService interactService, KeywordConfig keywordConfig, BotUtil botUtil, DifyClient difyClient) {
        this.interactService = interactService;
        this.keywordConfig = keywordConfig;
        this.botUtil = botUtil;
        this.difyClient = difyClient;
    }

    @Override
    public String textMsgHandle(BaseMsg baseMsg) {
        //如果是在群聊
        if (baseMsg.isGroupMsg()){
            //存在@机器人的消息就向ChatGPT提问
            if (baseMsg.getText().contains("@"+ Core.getInstance().getNickName())){
                //去除@再提问
                String prompt = baseMsg.getText().replace("@"+ Core.getInstance().getNickName() + " ", "").trim();
                return textResponse(baseMsg.getFromUserName(), prompt);
            }
        }else {
            //不是在群聊 则直接回复
            return textResponse(baseMsg.getFromUserName(), baseMsg.getText());
        }
        return null;
    }

    private String textResponse(String userName, String content) {
        if (keywordConfig.getReset().equals(content)){
//            botUtil.resetPrompt(userName);

            difyClient.reset(userName);
            return "重置会话成功";
        }else {
            ChatBO chatBO = new ChatBO();
            chatBO.setPrompt(content);
            chatBO.setSessionId(userName);
            if (content.startsWith(keywordConfig.getImage())) {
                chatBO.setPrompt(content.replaceFirst(keywordConfig.getImage() + " ", ""));
                chatBO.setChatType(ChatType.IMAGE);
            } else if (content.startsWith(keywordConfig.getAudio())) {
                return "微信暂不支持语音回复";
            } else {
//                chatBO.setPrompt(content);
//                chatBO.setChatType(ChatType.TEXT);


//                 Create an instance of a model
//                ChatLanguageModel model = OpenAiChatModel.withApiKey("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");
//                return model.generate(content);

                // 调用dify提供的接口
//                return postDify(content);
                return difyClient.getFromDify(userName, content);
            }
            return interactService.chat(chatBO).getStringResult();
        }
    }

    @Override
    public String picMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String voiceMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String viedoMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String nameCardMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public void sysMsgHandle(BaseMsg baseMsg) {
    }

    @Override
    public String verifyAddFriendMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    @Override
    public String mediaMsgHandle(BaseMsg baseMsg) {
        return null;
    }

    private String postDify(String msg) {
        String api = "/chat-messages";
        String url = String.format("%s%s", "http://127.0.0.1:5001/v1", api);

        // 创建HttpClient示例
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // 创建HttpPost示例
        HttpPost httpPost = new HttpPost("http://httpbin.org/post");
        try {
            httpPost.setEntity(new StringEntity("this is Post"));
        } catch (UnsupportedEncodingException e) {
            return e.toString();
        }

        // 调用HttpClient实例来执行HttpPost示例
        try {
            CloseableHttpResponse response = httpClient.execute(httpPost);
            // 读response
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                System.out.println(response);
                System.out.println("=================");
                String html = EntityUtils.toString(entity);
                System.out.println(html);

                // 释放连接
                response.close();
                httpClient.close();

                return html;
            } else {
                throw new ClientProtocolException("Unexpected response status code: " + status);
            }
        } catch (IOException e) {
            return e.toString();
        }
    }
}
