package com.ashin.handler;

import com.ashin.config.QqConfig;
import com.ashin.entity.bo.ChatBO;
import com.ashin.exception.ChatException;
import com.ashin.service.InteractService;
import com.ashin.util.BotUtil;
import net.mamoe.mirai.contact.MessageTooLargeException;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.NewFriendRequestEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * QQ消息处理程序
 *
 * @author ashinnotfound
 * @date 2023/2/1
 */
@Component
public class QqMessageHandler implements ListenerHost {
    @Resource
    private InteractService interactService;
    @Resource
    private QqConfig qqConfig;

    private static final String RESET_WORD = "重置会话";

    /**
     * 监听消息并把ChatGPT的回答发送到对应qq/群
     * 注：如果是在群聊则需@
     *
     * @param event 事件 ps:此处是MessageEvent 故所有的消息事件都会被监听
     */
    @EventHandler
    public void onMessage(@NotNull MessageEvent event){
        ChatBO chatBO = new ChatBO();
        chatBO.setSessionId(String.valueOf(event.getSubject().getId()));
        if (event.getBot().getGroups().contains(event.getSubject().getId())) {
            //如果是在群聊
            if (event.getMessage().contains(new At(event.getBot().getId()))) {
                //存在@机器人的消息就向ChatGPT提问
                //去除@再提问
                String prompt = event.getMessage().contentToString().replace("@" + event.getBot().getId(), "").trim();
                response(event, chatBO, prompt);
            }
        } else {
            //不是在群聊 则直接回复
            String prompt = event.getMessage().contentToString().trim();
            response(event, chatBO, prompt);
        }
    }
    private void response(@NotNull MessageEvent event, ChatBO chatBO, String prompt) {
        if (RESET_WORD.equals(prompt)) {
            //检测到重置会话指令
            BotUtil.resetPrompt(chatBO.getSessionId());
            event.getSubject().sendMessage("重置会话成功");
        } else {
            String response;
            try {
                chatBO.setPrompt(prompt);
                response = interactService.chat(chatBO);
            }catch (ChatException e){
                response = e.getMessage();
            }
            try {
                MessageChain messages = new MessageChainBuilder()
                        .append(new QuoteReply(event.getMessage()))
                        .append(response)
                        .build();
                event.getSubject().sendMessage(messages);
            }catch (MessageTooLargeException e){
                //信息太大，无法引用，采用直接回复
                event.getSubject().sendMessage(response);
            }
        }
    }

    @EventHandler
    public void onNewFriendRequestEvent(NewFriendRequestEvent event){
        if (qqConfig.getAcceptNewFriend()){
            event.accept();
        }
    }
    @EventHandler
    public void onNewGroupRequestEvent(BotInvitedJoinGroupRequestEvent event){
        if (qqConfig.getAcceptNewGroup()){
            event.accept();
        }
    }
}