import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;



public class TextGraphProcessorExtraTest {
    private TextGraphProcessorExtra processor;

    @Before
    public void setUp() throws IOException {
        processor = new TextGraphProcessorExtra();
        // 使用示例文本初始化图
        processor.processTextFile("D:\\JavaProject\\Software\\Lab1_v1\\src\\Easy Test.txt");
    }

    // 测试用例1: 空输入
    @Test
    public void testGenerateNewText_EmptyInput() {
        String input = "";
        String result = processor.generateNewText(input);
        assertEquals("", result);
    }

    // 测试用例2: 单个单词输入
    @Test
    public void testGenerateNewText_SingleWord() {
        String input = "Hello";
        String result = processor.generateNewText(input);
        assertEquals("Hello", result);
    }

    // 测试用例3: 两个单词输入，无桥接词
    @Test
    public void testGenerateNewText_TwoWordsNoBridge() {
        String input = "team requested";
        String result = processor.generateNewText(input);
        assertEquals("team requested", result);
    }

    // 测试用例4: 两个单词输入，有桥接词
    @Test
    public void testGenerateNewText_TwoWordsWithBridge() {
        String input = "scientist analyzed";
        // 假设"scientist -> analyzed"之间有桥接词"carefully"
        String result = processor.generateNewText(input);
        assertNotEquals(input, result);
        assertTrue(result.contains("carefully"));
    }

    // 测试用例5: 多个单词输入，混合大小写和标点
    @Test
    public void testGenerateNewText_MixedCaseWithPunctuation() {
        String input = "The Scientist analyzed, wrote and shared!";
        String result = processor.generateNewText(input);
        // 检查是否保留了原始大小写和标点
        assertTrue(result.startsWith("The Scientist"));
        assertTrue(result.contains(",") || result.contains("!"));
        // 检查是否插入了桥接词
        assertNotEquals(input, result);
    }

    // 测试用例6: 长文本输入，测试性能
    @Test(timeout = 1000) // 1秒超时
    public void testGenerateNewText_LongTextPerformance() {
        StringBuilder longText = new StringBuilder();
        // 重复示例文本多次创建长文本
        for (int i = 0; i < 100; i++) {
            longText.append("The scientist analyzed the data ");
        }
        String result = processor.generateNewText(longText.toString());
        assertNotNull(result);
        assertTrue(result.length() > longText.length());
    }

    // 测试用例7: 非字母字符输入
    @Test
    public void testGenerateNewText_NonAlphabeticInput() {
        String input = "123 !@# $%^";
        String result = processor.generateNewText(input);
        assertEquals("123 !@# $%^", result); // 应该保持不变
    }

    // 测试用例8: 原始示例文本
    @Test
    public void testGenerateNewText_OriginalExample() {
        String input = "The scientist carefully analyzed the data, wrote a detailed report, " +
                "and shared the report with the team, but the team requested more data, " +
                "so the scientist analyzed it again";
        String result = processor.generateNewText(input);
        // 结果应该与输入不同，因为会插入额外的桥接词
        assertNotEquals(input, result);
        // 检查原始桥接词是否仍然存在
        assertTrue(result.contains("carefully"));
        assertTrue(result.contains("detailed"));
    }

    // 测试用例9: 检查桥接词插入逻辑
    @Test
    public void testGenerateNewText_BridgeWordInsertion() {
        String input = "a report";
        String result = processor.generateNewText(input);
        // 检查是否在"scientist"和"report"之间插入了桥接词
        assertTrue(result.contains("a") && result.contains("report"));
        assertTrue(result.indexOf("a") < result.indexOf("report"));
        // 桥接词应该出现在两者之间
        String[] parts = result.split("\\s+");
        int scientistIndex = -1;
        int reportIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("a")) scientistIndex = i;
            if (parts[i].equalsIgnoreCase("report")) reportIndex = i;
        }
        assertTrue(scientistIndex >= 0 && reportIndex > scientistIndex);
        // 检查两者之间是否有桥接词
        assertTrue(reportIndex - scientistIndex > 1);
    }

    // 测试用例10: 保留原始文本格式
    @Test
    public void testGenerateNewText_PreserveFormatting() {
        String input = "  The   scientist  analyzed  "; // 多个空格
        String result = processor.generateNewText(input);
        // 检查是否保留了原始单词顺序和数量
        String[] inputWords = input.trim().split("\\s+");
        String[] resultWords = result.trim().split("\\s+");
        assertEquals(inputWords.length, resultWords.length - 1); // 可能插入一个桥接词
        assertEquals("The", resultWords[0]);
        assertEquals("scientist", resultWords[1]);
    }

    // 测试用例11: 保留双引号
    @Test
        public void testGenerateNewText_PreserveQuotationMarks() {
            String input = "\" The scientist analyzed the data \""; // 输入含双引号
            String result = processor.generateNewText(input);

            // 检查输出仍然包含两端的双引号
            assertTrue("输出未保留起始双引号", result.startsWith("\""));
            assertTrue("输出未保留结束双引号", result.endsWith("\""));

            // 原始单词应都还在
            assertTrue(result.contains("scientist"));
            assertTrue(result.contains("analyzed"));
        }

}