import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 文本图处理器，用于构建和处理文本的有向图表示。.
 * 提供多种图算法操作，包括桥接词查找、最短路径计算、PageRank计算等。
 *
 * <p>主要功能包括：
 * <ul>
 *   <li>从文本文件构建有向图</li>
 *   <li>可视化图结构</li>
 *   <li>查询节点度数和边权重</li>
 *   <li>查找桥接词</li>
 *   <li>生成包含桥接词的新文本</li>
 *   <li>计算最短路径</li>
 *   <li>执行随机游走</li>
 *   <li>计算PageRank值</li>
 * </ul>
 *
 * @author 作者名
 * @version 1.0
 */
public class TextGraphProcessorExtra {
    /** 存储有向图结构的邻接表. */
    private Map<String, Map<String, Integer>> graph;

    /** 存储处理后的单词列表. */
    private List<String> words;

    /** 默认的PageRank阻尼系数. */
    private static final double DEFAULT_D = 0.85;
    /** 默认的最大迭代次数. */
    private static final int DEFAULT_MAX_ITER = 100;
    /** 默认的收敛容差. */
    private static final double DEFAULT_TOL = 1e-6;

    /**
     * 构造一个新的文本图处理器。.
     */
    public TextGraphProcessorExtra() {
        graph = new HashMap<>();
        words = new ArrayList<>();
    }

    /**
     * 读取并处理文本文件，构建有向图。.
     *
     * @param filePath 要处理的文本文件路径
     * @throws IOException 如果读取文件时发生I/O错误
     */
    public void processTextFile(String filePath) throws IOException {
        // 获取项目根目录
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path inputPath = projectRoot.resolve(filePath).normalize();

        // 确保路径未逃逸出项目目录
        if (!inputPath.startsWith(projectRoot)) {
            throw new SecurityException("不允许访问项目目录外的文件: " + filePath);
        }

        // 读取文件内容
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }
        }

        // 处理文本 + 构建图
        processText(content.toString());
        buildGraph();
    }

    /**
     * 处理文本内容，将其转换为单词序列。.
     *
     * @param text 要处理的原始文本
     */
    private void processText(String text) {
        // 替换所有非字母字符为空格，并转换为小写
        String processed = text.replaceAll("[^a-zA-Z]", " ").toLowerCase();
        // 分割单词，处理连续空格
        String[] tokens = processed.split("\\s+");

        words.clear();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                words.add(token);
            }
        }
    }

    /**
     * 根据处理后的单词序列构建有向图。.
     */
    private void buildGraph() {
        graph.clear();

        if (words.size() < 2) {
            return; // 至少需要两个单词才能构建边
        }

        // 1. 先把所有出现过的单词都当作节点，加到 graph 里
        for (String w : words) {
            graph.putIfAbsent(w, new HashMap<>());
        }

        // 2. 再遍历相邻单词对，累加它们之间的有向边权重
        for (int i = 0; i < words.size() - 1; i++) {
            String current = words.get(i);
            String next    = words.get(i + 1);

            Map<String, Integer> edges = graph.get(current);
            edges.put(next, edges.getOrDefault(next, 0) + 1);
        }
    }


    /**
     * 显示有向图的结构。.
     *
     */
    public void showDirectedGraph() {
        System.out.println("有向图结构:");
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String source = entry.getKey();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                System.out.printf("%s -> %s [weight=%d]%n",
                        source, edge.getKey(), edge.getValue());
            }
        }
    }

    /**
     * 将有向图保存为图片文件。.
     *
     * @param outputPath 输出图片文件路径
     * @throws IOException 如果生成图片时发生I/O错误
     */
    public void saveGraphAsImage(String outputPath) throws IOException {
        // 生成DOT文件内容
        StringBuilder dotContent = new StringBuilder();
        dotContent.append("digraph G {\n");
        dotContent.append("    rankdir=LR;\n"); // 从左到右布局
        dotContent.append("    node [shape=circle];\n");

        // 添加所有节点和边
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String source = entry.getKey();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                String target = edge.getKey();
                int weight = edge.getValue();
                dotContent.append(String.format("    \"%s\" -> \"%s\" [label=\"%d\"];%n",
                        source, target, weight));
            }
        }

        dotContent.append("}\n");

        // 将DOT内容写入临时文件
        File dotFile = File.createTempFile("graph", ".dot");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(dotFile), StandardCharsets.UTF_8))) {
            writer.write(dotContent.toString());
        }

        // 调用Graphviz生成图片
        try {
            Path projectRoot = Paths.get("").toAbsolutePath().normalize();
            Path outputImgPath = projectRoot.resolve(outputPath).normalize();
            if (!outputImgPath.startsWith(projectRoot)) {
                throw new SecurityException("不允许写入项目目录外的文件: " + outputPath);
            }
            ProcessBuilder pb = new ProcessBuilder(
                    "dot",
                    "-Tpng",
                    dotFile.getAbsolutePath(),
                    "-o",
                    outputImgPath.toString()
            );
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Graphviz生成图片失败，请确保已安装Graphviz并添加到系统PATH");
                // 读取错误流
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
            } else {
                System.out.println("图形已保存为: " + outputPath);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("生成图片过程被中断");
        } catch (IOException e) {
            System.err.println("执行Graphviz时出错: " + e.getMessage());
        } finally {
            if (!dotFile.delete()) {
                System.err.println("警告: 临时文件未能成功删除: " + dotFile.getAbsolutePath());
            }
        }
    }

    /**
     * 计算指定单词的出度。.
     *
     * @param word 要查询的单词
     * @return 该单词的出度
     */
    public int calculateOutDegree(String word) {
        word = word.toLowerCase();
        if (!graph.containsKey(word)) {
            return 0;
        }
        return graph.get(word).size();
    }

    /**
     * 计算指定单词的入度。.
     *
     * @param word 要查询的单词
     * @return 该单词的入度
     */
    public int calculateInDegree(String word) {
        word = word.toLowerCase();
        int count = 0;
        for (Map<String, Integer> edges : graph.values()) {
            if (edges.containsKey(word)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取两个单词之间的边权重。.
     *
     * @param source 源单词
     * @param target 目标单词
     * @return 边的权重，如果没有边则返回0
     */
    public int getEdgeWeight(String source, String target) {
        source = source.toLowerCase();
        target = target.toLowerCase();
        if (!graph.containsKey(source)) {
            return 0;
        }
        return graph.get(source).getOrDefault(target, 0);
    }

    /**
     * 查询两个单词之间的桥接词。.
     *
     * @param word1 第一个单词
     * @param word2 第二个单词
     * @return 描述桥接词的字符串结果
     */
    public String queryBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        // 检查word1和word2是否在图中
        boolean word1Exists = graph.containsKey(word1);
        boolean word2Exists = graph.containsKey(word2);

        if (!word1Exists || !word2Exists) {
            // 处理两个词都不在的情况
            if (!word1Exists && !word2Exists) {
                return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
            } else {
                // 处理其中一个词不在的情况
                return "No " + (!word1Exists ? "\"" + word1 + "\"" : "\"" + word2 + "\"")
                        + " in the graph!";
            }
        }

        List<String> bridges = findBridgeWords(word1, word2);

        if (bridges.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        } else {
            String verb = bridges.size() == 1 ? "is" : "are"; // 根据数量选择is/are
            StringBuilder output = new StringBuilder("The bridge word");
            output.append(bridges.size() == 1 ? "" : "s")
                    .append(" from ")
                    .append(word1)
                    .append(" to ")
                    .append(word2)
                    .append(" ")
                    .append(verb)
                    .append(": ");

            if (bridges.size() == 1) {
                output.append(bridges.get(0)).append(".");
            } else {
                for (int i = 0; i < bridges.size(); i++) {
                    if (i == bridges.size() - 1) {
                        output.append("and ").append(bridges.get(i)).append(".");
                    } else {
                        output.append(bridges.get(i)).append(", ");
                    }
                }
            }
            return output.toString();
        }
    }

    /**
     * 查找两个单词之间的所有桥接词。.
     *
     * @param word1 第一个单词
     * @param word2 第二个单词
     * @return 桥接词列表
     */
    private List<String> findBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
        List<String> bridges = new ArrayList<>();

        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return bridges;
        }

        // 获取word1的所有出边节点
        Set<String> word1Neighbors = graph.get(word1).keySet();

        for (String candidate : word1Neighbors) {
            if (graph.containsKey(candidate) && graph.get(candidate).containsKey(word2)) {
                bridges.add(candidate);
            }
        }

        return bridges;
    }

    /**
     * 生成包含桥接词的新文本。.
     *
     * @param inputText 输入文本
     * @return 包含桥接词的新文本
     */
    public String generateNewText(String inputText) {
        // 原始文本分词，保留标点
        String[] tokens = inputText.split("(?<=\\b)(?=\\b)|(?<=\\W)(?=\\w)|(?<=\\w)(?=\\W)");

        // 提取纯单词并转换为小写用于查找桥接词
        List<String> validWords = new ArrayList<>();
        for (String token : tokens) {
            if (token.matches("[a-zA-Z]+")) {
                validWords.add(token.toLowerCase());
            }
        }

        if (validWords.size() < 2) {
            return inputText;
        }

        // 构建结果
        StringBuilder result = new StringBuilder();
        int wordIndex = 0;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];

            if (token.matches("[a-zA-Z]+") && wordIndex < validWords.size() - 1) {
                String current = validWords.get(wordIndex);
                String next = validWords.get(wordIndex + 1);

                result.append(token); // 添加原始单词
                // 查找桥接词并插入
                List<String> bridges = findBridgeWords(current, next);
                if (!bridges.isEmpty()) {
                    String bridge = bridges.get(
                            ThreadLocalRandom.current().nextInt(bridges.size()));
                    result.append(" ").append(bridge); // 插入桥接词
                }
                result.append(" ");
                wordIndex++;
            } else {
                result.append(token);
            }
        }

        return result.toString().replaceAll("\\s+", " ").trim();
    }


    /**
     * 计算两个单词之间的最短路径。.
     *
     * @param word1 起始单词
     * @param word2 目标单词
     * @return 描述最短路径的字符串
     */

    public String calcShortestPath(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return "No path found.";
        }

        // Dijkstra算法实现（按权重计算最短路径）
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        // 初始化
        for (String node : graph.keySet()) {
            if (node.equals(word1)) {
                distances.put(node, 0);
            } else {
                distances.put(node, Integer.MAX_VALUE);
            }
            queue.add(new Node(node, distances.get(node)));
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            // 提前终止如果找到目标节点
            if (current.word.equals(word2)) {
                break;
            }

            // 跳过已处理过的更短路径
            if (current.distance > distances.get(current.word)) {
                continue;
            }

            if (!graph.containsKey(current.word)) {
                continue;
            }

            // 遍历所有邻居
            for (Map.Entry<String, Integer> edge : graph.get(current.word).entrySet()) {
                String neighbor = edge.getKey();
                int weight = edge.getValue();
                int altDistance = distances.get(current.word) + weight;

                // 发现更短路径
                if (altDistance < distances.get(neighbor)) {
                    distances.put(neighbor, altDistance);
                    previous.put(neighbor, current.word);
                    queue.add(new Node(neighbor, altDistance));
                }
            }
        }

        // 构建路径（从目标回溯）
        List<String> path = new LinkedList<>();
        String current = word2;

        // 检查是否找到路径
        if (previous.get(current) == null && !word1.equals(word2)) {
            return "No path found from " + word1 + " to " + word2 + ".";
        }

        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        // 将路径转换为字符串格式
        return String.join(" -> ", path);
    }

    /**
     * 计算从指定单词到所有其他单词的最短路径。.
     *
     * @param startWord 起始单词
     * @return 包含所有最短路径的映射，键为目标单词，值为路径字符串
     */
    public Map<String, String> calcAllShortestPathsFrom(String startWord) {
        Map<String, String> allPaths = new HashMap<>();
        startWord = startWord.toLowerCase();

        if (!graph.containsKey(startWord)) {
            return allPaths;
        }

        // 使用Dijkstra算法计算单源最短路径
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        // 初始化
        for (String node : graph.keySet()) {
            if (node.equals(startWord)) {
                distances.put(node, 0);
            } else {
                distances.put(node, Integer.MAX_VALUE);
            }
            queue.add(new Node(node, distances.get(node)));
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            // 跳过已处理过的更短路径
            if (current.distance > distances.get(current.word)) {
                continue;
            }

            if (!graph.containsKey(current.word)) {
                continue;
            }

            // 遍历所有邻居
            for (Map.Entry<String, Integer> edge : graph.get(current.word).entrySet()) {
                String neighbor = edge.getKey();
                int weight = edge.getValue();
                int altDistance = distances.get(current.word) + weight;

                // 发现更短路径
                if (altDistance < distances.get(neighbor)) {
                    distances.put(neighbor, altDistance);
                    previous.put(neighbor, current.word);
                    queue.add(new Node(neighbor, altDistance));
                }
            }
        }

        // 为每个目标节点构建路径字符串
        for (String target : graph.keySet()) {
            if (target.equals(startWord)) {
                continue;
            }

            // 构建路径（从目标回溯）
            List<String> path = new LinkedList<>();
            String current = target;

            // 检查是否找到路径
            if (previous.get(current) != null || target.equals(startWord)) {
                while (current != null) {
                    path.add(0, current);
                    current = previous.get(current);
                }
                allPaths.put(target, String.join(" -> ", path));
            }
        }

        return allPaths;
    }

    /**
     * 计算图中所有节点的PageRank值。.
     *
     * @param d 阻尼系数
     * @param maxIter 最大迭代次数
     * @param tol 收敛容差
     * @return 包含每个节点PageRank值的映射
     */
    public Map<String, Double> calculatePageRank(double d, int maxIter, double tol) {
        int verticesNum = graph.size();
        Map<String, Double> pr = initializePageRankByTf();

        // 找出所有出度为0的节点（dangling nodes）
        Set<String> danglingNodes = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            if (entry.getValue().isEmpty()) {
                danglingNodes.add(entry.getKey());
            }
        }


        // 迭代计算
        for (int iter = 0; iter < maxIter; iter++) {
            Map<String, Double> newPr = new HashMap<>();
            double diff = 0;
            double danglingSum = 0.0;

            // 计算所有dangling nodes的PR值总和
            for (String node : danglingNodes) {
                danglingSum += pr.get(node);
            }

            // 将danglingSum均分给所有节点
            double danglingContribution = danglingSum / verticesNum;

            for (String u : graph.keySet()) {
                double sum = 0;

                // 遍历所有指向u的节点v
                for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
                    String v = entry.getKey();
                    Map<String, Integer> edges = entry.getValue();
                    if (edges.containsKey(u)) {
                        int outCount = edges.size();
                        if (outCount > 0) {
                            sum += pr.get(v) / outCount;
                        }
                    }
                }

                // 添加dangling nodes的贡献
                sum += danglingContribution;

                double prValue = (1 - d) / verticesNum + d * sum;
                newPr.put(u, prValue);
                diff += Math.abs(prValue - pr.get(u));
            }

            pr = newPr;
            if (diff < tol) {
                break;
            }
        }
        return pr;
    }

    /**
     * 计算指定单词的PageRank值。.
     *
     * @param word 要查询的单词
     * @return 该单词的PageRank值
     */
    public Double calPageRank(String word) {
        if (graph.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> prMap = calculatePageRank(DEFAULT_D, DEFAULT_MAX_ITER, DEFAULT_TOL);
        return prMap.getOrDefault(word.toLowerCase(), 0.0);
    }

    /**
     * 根据词频初始化PageRank值。.
     *
     * @return 包含初始PageRank值的映射
     */
    Map<String, Double> initializePageRankByTf() {
        Map<String, Integer> wordCount = new HashMap<>();
        int totalWords = 0;
        for (String w : words) {
            wordCount.put(w, wordCount.getOrDefault(w, 0) + 1);
            totalWords++;
        }

        Map<String, Double> pr = new HashMap<>();
        for (String node : graph.keySet()) {
            int count = wordCount.getOrDefault(node, 0);
            pr.put(node, (double) count / totalWords);
        }
        return pr;
    }

    /**
     * 表示图中的节点，用于最短路径计算。.
     */
    private static class Node {
        /** 节点对应的单词. */
        String word;
        /** 到该节点的距离. */
        int distance;

        /**
         * 构造一个新的节点。.
         *
         * @param word 节点单词
         * @param distance 到该节点的距离
         */
        public Node(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }
    }

    /**
     * 在图中执行随机游走。.
     *
     * @return 游走路径的字符串表示
     * @throws IOException 如果写入文件时发生I/O错误
     */
    public String randomWalk() throws IOException {
        if (graph.isEmpty()) {
            return "";
        }

        SecureRandom random = new SecureRandom();
        List<String> nodes = new ArrayList<>(graph.keySet());
        String current = nodes.get(random.nextInt(nodes.size()));

        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("walk.txt"), StandardCharsets.UTF_8));

        // 记录起点
        System.out.println("起点节点: " + current);
        writer.write("节点: " + current);
        writer.newLine();
        StringBuilder sb = new StringBuilder();
        Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            Map<String, Integer> outEdges = graph.get(current);
            if (outEdges == null || outEdges.isEmpty()) {
                System.out.println("当前节点无出边，遍历结束。");
                break;
            }
            // 选一条随机出边
            List<String> neigh = new ArrayList<>(outEdges.keySet());
            String next = neigh.get(random.nextInt(neigh.size()));
            String edge = current + "->" + next;

            // 如果这条边已出现过，则结束
            writer.flush();  // 确保上一轮写入
            List<String> existing = Files.readAllLines(Paths.get("walk.txt"));
            if (existing.contains("边: " + edge)) {
                System.out.println("检测到重复边 " + edge + "，遍历结束。");
                break;
            }

            // 用户是否想中断？
            System.out.print("按 Enter 继续，输入 q 并回车结束 traversal: ");
            String line = sc.nextLine();
            if ("q".equalsIgnoreCase(line.trim())) {
                System.out.println("用户中断，遍历结束。");
                break;
            }

            // 写入并打印这条边和下一个节点
            System.out.println("边: " + edge);
            writer.write("边: " + edge);
            writer.newLine();

            sb.append(next).append(" ");
            System.out.println("节点: " + next);
            writer.write("节点: " + next);
            writer.newLine();

            current = next;
        }

        writer.close();
        System.out.println("遍历结果已保存到 walk.txt");
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 主方法，提供交互式命令行界面。.
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        TextGraphProcessorExtra processor = new TextGraphProcessorExtra();
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

        try {
            // 直接指定文件路径为input.txt
            String filePath = "D:\\JavaProject\\Software\\Lab1_v1\\src\\Easy Test.txt";
            System.out.println("正在读取文件: " + filePath);
            processor.processTextFile(filePath);

            System.out.println("\n文本处理完成，有向图已构建。");
            processor.showDirectedGraph();

            // 新增：保存图形为图片
            System.out.println("\n正在生成图形文件...");
            processor.saveGraphAsImage("graph.png");

            boolean running = true;
            while (running) {
                System.out.println("\n请选择操作:");
                System.out.println("1. 查询节点出度");
                System.out.println("2. 查询节点入度");
                System.out.println("3. 查询边权重");
                System.out.println("4. 查找桥接词");
                System.out.println("5. 生成新文本");
                System.out.println("6. 计算最短路径");
                System.out.println("7. 随机游走");
                System.out.println("8. 计算PageRank");
                System.out.println("9. 重新生成图形文件");
                System.out.println("10. 退出");
                System.out.print("请输入选项: ");

                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("无效输入，请输入数字1-10。");
                    continue;
                }

                switch (choice) {
                    case 1:
                        System.out.print("请输入单词: ");
                        String word = scanner.nextLine();
                        System.out.printf("'%s'的出度为: %d%n",
                                word, processor.calculateOutDegree(word));
                        break;
                    case 2:
                        System.out.print("请输入单词: ");
                        word = scanner.nextLine();
                        System.out.printf("'%s'的入度为: %d%n",
                                word, processor.calculateInDegree(word));
                        break;
                    case 3:
                        System.out.print("请输入源单词: ");
                        String source = scanner.nextLine();
                        System.out.print("请输入目标单词: ");
                        String target = scanner.nextLine();
                        System.out.printf("'%s' -> '%s'的权重为: %d%n",
                                source, target, processor.getEdgeWeight(source, target));
                        break;
                    case 4:
                        System.out.print("请输入第一个单词: ");
                        String word1 = scanner.nextLine();
                        System.out.print("请输入第二个单词: ");
                        String word2 = scanner.nextLine();
                        String result4 =  processor.queryBridgeWords(word1, word2);
                        System.out.println(result4);
                        break;
                    case 5:
                        System.out.print("请输入新文本: ");
                        String newText = scanner.nextLine();
                        System.out.println("生成的新文本: " + processor.generateNewText(newText));
                        break;
                    case 6:
                        System.out.print("请输入起始单词: ");
                        word1 = scanner.nextLine();
                        System.out.print("请输入目标单词(留空则计算到所有单词的最短路径): ");
                        word2 = scanner.nextLine().trim();

                        if (word2.isEmpty()) {
                            // 单单词情况：计算到所有其他单词的最短路径
                            Map<String, String> allPaths =
                                    processor.calcAllShortestPathsFrom(word1);
                            if (allPaths.isEmpty()) {
                                System.out.println("没有找到路径。");
                            } else {
                                System.out.println("\n" + word1 + " 到其他单词的最短路径:");
                                for (Map.Entry<String, String> entry :
                                        allPaths.entrySet()) {
                                    System.out.println(
                                            "到 " + entry.getKey() + ": " + entry.getValue());
                                    System.out.println("------");
                                }
                            }
                        } else {
                            // 双单词情况：原有功能
                            String path = processor.calcShortestPath(word1, word2);
                            System.out.println("最短路径: " + path);
                        }
                        break;
                    case 7:
                        try {
                            String result = processor.randomWalk();
                            System.out.println(result);
                        } catch (IOException e) {
                            System.err.println("写文件出错: " + e.getMessage());
                        }
                        break;
                    case 8:
                        System.out.print("请输入计算单词: ");
                        String wordToCal = scanner.nextLine();
                        double prValue = processor.calPageRank(wordToCal);
                        System.out.println(wordToCal + " 的 PageRank = " + prValue);
                        break;
                    case 9:
                        System.out.print("请输入输出文件名(如graph.png): ");
                        String outputFile = scanner.nextLine();
                        try {
                            processor.saveGraphAsImage(outputFile);
                        } catch (IOException e) {
                            System.err.println("生成图形文件时出错: " + e.getMessage());
                        }
                        break;
                    case 10:
                        running = false;
                        break;
                    default:
                        System.out.println("无效选项，请输入1-10之间的数字。");
                }
            }
        } catch (IOException e) {
            System.err.println("处理文件时出错: " + e.getMessage());
            System.err.println("请确保input.。");
        } finally {
            scanner.close();
        }
    }
}