package com.example.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.*;
import java.util.Arrays;
import java.util.HashMap;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.IMAPFolder;
import javax.mail.FetchProfile;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.SearchTerm;
import com.example.email.entity.Email;
import com.example.email.repository.EmailRepository;

@Slf4j
@Service
public class EmailService {
    @Value("${email.username}")
    private String username;

    @Value("${email.password}")
    private String password;

    @Value("${email.host}")
    private String host;

    @Value("${email.protocol}")
    private String protocol;

    private final Session emailSession;

    @Autowired
    private EmailRepository emailRepository;

    public EmailService(Session emailSession) {
        this.emailSession = emailSession;
    }

    @Scheduled(fixedRate = 30000)
    public void processEmails() {
        IMAPStore store = null;
        IMAPFolder inbox = null;
        try {
            // 这部分就是解决异常的关键所在，设置IAMP ID信息
            HashMap<String, String> IAM = new HashMap<>();
            // 带上IMAP ID信息，由key和value组成，例如name，version，vendor，support-email等。
            // 这个value的值随便写就行
            IAM.put("name", "myname");
            IAM.put("version", "1.0.0");
            IAM.put("vendor", "myclient");
            IAM.put("support-email", "testmail@test.com");

            store = (IMAPStore) emailSession.getStore(protocol);
            store.connect(host, username, password);
            store.id(IAM);

            // 获取收件箱
            inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // 直接获取所有邮件
            Message[] messages = inbox.getMessages();

            // 设置获取属性
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            profile.add(FetchProfile.Item.FLAGS);
            profile.add(FetchProfile.Item.CONTENT_INFO);
            inbox.fetch(messages, profile);

            log.info("收件箱中共有 {} 封邮件", messages.length);

            // 确保目标文件夹存在
            Folder errorFolder = store.getFolder("error");
            Folder successFolder = store.getFolder("success");

            if (!errorFolder.exists()) {
                errorFolder.create(Folder.HOLDS_MESSAGES);
            }
            if (!successFolder.exists()) {
                successFolder.create(Folder.HOLDS_MESSAGES);
            }

            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];
                String subject = message.getSubject();
                String from = Arrays.toString(message.getFrom());
                String sentDate = message.getSentDate() != null ? message.getSentDate().toString() : "未知";

                log.info("------------------------");
                log.info("邮件 #{}", (i + 1));
                log.info("主题: {}", subject);
                log.info("发件人: {}", from);
                log.info("发送时间: {}", sentDate);

                // 保存到数据库
                Email email = new Email();
                email.setSubject(subject);
                email.setSender(from);
                emailRepository.save(email);

                // 移动邮件到相应文件夹
                if (from.toLowerCase().contains("jay")) {
                    errorFolder.appendMessages(new Message[] { message });
                } else {
                    successFolder.appendMessages(new Message[] { message });
                }
                // 标记原邮件为删除
                message.setFlag(Flags.Flag.DELETED, true);
            }

        } catch (Exception e) {
            log.error("处理邮件时发生错误", e);
            log.error("错误详情：{}", e.getMessage());
            if (e.getCause() != null) {
                log.error("根本原因：{}", e.getCause().getMessage());
            }
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(true); // true 表示删除标记的邮件
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException ex) {
                log.error("关闭连接时发生错误", ex);
            }
        }
    }
}