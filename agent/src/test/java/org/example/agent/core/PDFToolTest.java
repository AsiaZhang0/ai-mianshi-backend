package org.example.agent.core;

import org.example.agent.AgentApplication;
import org.example.agent.core.service.TeacherService;
import org.example.agent.core.tools.PDFTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AgentApplication.class)
public class PDFToolTest {

    @Autowired
    @Qualifier("teacherService")
    private TeacherService teacherService;

    @Autowired
    @Qualifier("teacherServiceStream")
    private TeacherService teacherServiceStream;

    @Test
    public void testReadPDF() throws Exception {
        ClassPathResource resource = new ClassPathResource("JAVA_张志明_硕士_26.pdf");
        PDFTool pdfTool = new PDFTool(resource);
        String content = pdfTool.readPDF();


        assertNotNull(content);
        assertTrue(content.length() > 0);
        System.out.println("PDF content length: " + content.length());
        System.out.println("First 500 chars: " + content.substring(0, Math.min(500, content.length())));
    }

    @Test
    public void testChatClient() throws Exception {
        ClassPathResource resource = new ClassPathResource("JAVA_张志明_硕士_26.pdf");
        PDFTool pdfTool = new PDFTool(resource);
        String content = pdfTool.readPDF();

        String chatResponse = teacherService.getPersonalInfo( content);
        // String chatResponse = teacherService.getPersonalInfo("你好\n");
        System.out.println("Chat response: " + chatResponse.toString());
    }


    @Test
    public void testChatClientStream() throws Exception {
        ClassPathResource resource = new ClassPathResource("JAVA_张志明_硕士_26.pdf");
        PDFTool pdfTool = new PDFTool(resource);
        String content = pdfTool.readPDF();

        // String chatResponse = teacherService.getPersonalInfo("请帮我分析一下这份简历的内容，提炼该用户的关键信息。简历内容如下：\n" + content);
        Flux<String> chatResponse = teacherServiceStream.getPersonalInfoStream( content);
        chatResponse.doOnNext(response -> System.out.println("Chat stream response: " + response)).blockLast();
    }

}

