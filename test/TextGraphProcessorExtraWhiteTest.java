import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

public class TextGraphProcessorExtraWhiteTest {
    private TextGraphProcessorExtra processor;

    @Before
    public void setUp() throws IOException {
        processor = new TextGraphProcessorExtra();
        processor.processTextFile("D:\\JavaProject\\Software\\Lab1_v1\\src\\Easy Test Extended.txt");
    }

    // 路径1：两个单词都不存在
    @Test
    public void testQueryBridgeWords_BothWordsNotExist() {
        String result = processor.queryBridgeWords("nonexistent1", "nonexistent2");
        assertEquals("No \"nonexistent1\" and \"nonexistent2\" in the graph!", result);
    }

    // 路径2：仅word1不存在
    @Test
    public void testQueryBridgeWords_FirstWordNotExist() {
        String result = processor.queryBridgeWords("nonexistent", "scientist");
        assertEquals("No \"nonexistent\" in the graph!", result);
    }

    // 路径2：仅word2不存在
    @Test
    public void testQueryBridgeWords_SecondWordNotExist() {
        String result = processor.queryBridgeWords("scientist", "nonexistent");
        assertEquals("No \"nonexistent\" in the graph!", result);
    }

    // 路径3：单词存在但无桥接词
    @Test
    public void testQueryBridgeWords_NoBridgeWords() {
        // "the" -> "data" 的直接连接，没有桥接词
        String result = processor.queryBridgeWords("the", "data");
        assertEquals("No bridge words from the to data!", result);

        // "team" -> "requested" 的直接连接
        result = processor.queryBridgeWords("team", "requested");
        assertEquals("No bridge words from team to requested!", result);
    }

    // 路径4：单词存在且有一个桥接词
    @Test
    public void testQueryBridgeWords_SingleBridgeWord() {
        // "the" -> "scientist" -> "carefully" -> "analyzed"
        // "the" -> "analyzed" 的桥接词是 "scientist"
        String result = processor.queryBridgeWords("the", "analyzed");
        assertEquals("The bridge word from the to analyzed is: scientist.", result);

        // "analyzed" -> "the" -> "data"
        // "analyzed" -> "data" 的桥接词是 "the"
        result = processor.queryBridgeWords("analyzed", "data");
        assertEquals("The bridge word from analyzed to data is: the.", result);
    }

    // 路径5：单词存在且有多个桥接词
    @Test
    public void testQueryBridgeWords_MultipleBridgeWords() {

        // "a" 和 "report" 之间有多个桥接词
        String result = processor.queryBridgeWords("a", "report");
        assertTrue(result.startsWith("The bridge words from a to report are: "));
        assertTrue(result.contains("detailed"));
        assertTrue(result.contains("complicated"));
        assertTrue(result.endsWith("."));

        // 验证输出格式是否正确
        if (result.contains("and")) {
            // 检查多桥接词格式
            assertTrue(result.matches("The bridge words from a to report are: (detailed, and complicated|complicated, and detailed)\\."));
        }
    }

    // 额外测试：大小写不敏感测试
    @Test
    public void testQueryBridgeWords_CaseInsensitivity() {
        String result1 = processor.queryBridgeWords("The", "Scientist");
        String result2 = processor.queryBridgeWords("the", "scientist");
        assertEquals(result2, result1);

        result1 = processor.queryBridgeWords("ANALYZED", "THE");
        result2 = processor.queryBridgeWords("analyzed", "the");
        assertEquals(result2, result1);
    }
}