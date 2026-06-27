package org.example.agent.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpSession;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.example.agent.core.InterviewAgentConfig;
import org.example.agent.core.service.InterviewAgentService;
import org.example.agent.core.service.TeacherService;
import org.example.agent.service.impl.FileServiceClient;
import org.example.common.exception.BusinessException;
import org.example.common.exception.ErrorCode;
import org.example.model.dto.file.FileUploadResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RestController
public class AgentController {

    private final TeacherService teacherServiceStream;
    private final FileServiceClient fileServiceClient;

    private final InterviewAgentConfig interviewAgentConfig;

    public AgentController(TeacherService teacherServiceStream,
                           FileServiceClient fileServiceClient,
                           InterviewAgentConfig interviewAgentConfig) {
        this.teacherServiceStream = teacherServiceStream;
        this.fileServiceClient = fileServiceClient;

        this.interviewAgentConfig = interviewAgentConfig;
    }

    /**
     * 上传简历PDF，先存储到腾讯云 COS，再流式返回用户画像
     * 前端通过 SSE (Server-Sent Events) 接收流式数据
     */
    @PostMapping(value = "/user-resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateUserResume(@RequestParam("file") MultipartFile file) {
        // 校验文件不为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }

        // 校验文件类型为 PDF
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持上传 PDF 格式文件");
        }

        // 解析 PDF 内容
        String resumeText;
        try {
            resumeText = parsePdf(file);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF 解析失败: " + e.getMessage());
        }

        if (resumeText == null || resumeText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "PDF 文件内容为空，无法解析");
        }

        // 异步上传 PDF 到腾讯云 COS（不阻塞 SSE 流返回）
        Mono.fromRunnable(() -> {
            try {
                FileUploadResult uploadResult = fileServiceClient.uploadPdf(file);
                // 可在日志中记录 COS URL，供后续查看/下载
            } catch (Exception e) {
                // 上传失败不影响 AI 分析主流程
            }
        }).subscribe();

        // 调用 AI 模型生成用户画像，返回 SSE 流
        return teacherServiceStream.getPersonalInfoStream(resumeText);
    }



    private static final String SESSION_KEY_MEMORY_ID = "interviewMemoryId";

    /**
     * Agent 多轮对话面试 —— 上传简历后开始面试
     * <p>
     * memoryId 通过 HttpSession 管理，前端无需传递，安全防篡改。
     *
     * @param file    简历 PDF（仅首次调用需要，后续传空）
     * @param message 用户当前消息（首次为问候语，后续为回答）
     * @return SSE 流式 AI 面试官回复
     */
    @PostMapping(value = "/interview/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentInterviewStart(@RequestParam(value = "file", required = false) MultipartFile file,
                                            @RequestParam(defaultValue = "你好，请开始面试") String message) {
        // 从 Session 获取或生成 memoryId

        String sessionId = UUID.randomUUID().toString();
        StpUtil.getSession().set(SESSION_KEY_MEMORY_ID, sessionId);


        String resumeText = "";
        if (file != null && !file.isEmpty()) {
            resumeText = parseAndValidatePdf(file);
            Mono.fromRunnable(() -> {
                try { fileServiceClient.uploadPdf(file); } catch (Exception ignored) { }
            }).subscribe();
        }

        InterviewAgentService agent = interviewAgentConfig.getOrCreateAgent(sessionId);
        return agent.interview(resumeText, message);
    }

    /**
     * Agent 多轮对话面试 —— 继续对话
     * <p>
     * memoryId 从 HttpSession 中读取，前端无需传递。
     *
     * @param message 用户当前消息
     * @return SSE 流式 AI 面试官回复
     */
    @PostMapping(value = "/interview/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentInterviewChat(@RequestParam String message) {
        String sessionId = (String) StpUtil.getSession().get(SESSION_KEY_MEMORY_ID);
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话不存在，请先调用 /interview/start 开始面试");
        }

        InterviewAgentService agent = interviewAgentConfig.getOrCreateAgent(sessionId);
        return agent.interview("", message);
    }

    /**
     * 解析并校验 PDF 文件
     */
    private String parseAndValidatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持上传 PDF 格式文件");
        }
        String resumeText;
        try {
            resumeText = parsePdf(file);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "PDF 解析失败: " + e.getMessage());
        }
        if (resumeText == null || resumeText.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "PDF 文件内容为空，无法解析");
        }
        return resumeText;
    }

    /**
     * 使用 Apache Tika 解析 PDF 文件内容
     */
    private String parsePdf(MultipartFile file) throws IOException, TikaException, SAXException {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        AutoDetectParser parser = new AutoDetectParser();

        try (InputStream inputStream = file.getInputStream()) {
            parser.parse(inputStream, handler, metadata, context);
        }

        return handler.toString();
    }
}
