import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

public class TextGraphProcessor {
    private Map<String, Map<String, Integer>> graph;
    private List<String> words;


    // 默认PageRank参数
    private static final double DEFAULT_D = 0.85;
    private static final int DEFAULT_MAX_ITER = 100;
    private static final double DEFAULT_TOL = 1e-6;


    public TextGraphProcessor() {
        graph = new HashMap<>();
        words = new ArrayList<>();
    }

    // 读取并处理文本文件
    public void processTextFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" "); // 将换行符替换为空格
            }
        }
        processText(content.toString());
        buildGraph();
    }

    // 处理文本内容
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

    // 构建有向图
// 构建有向图
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

    public Map<String, Map<String, Integer>> getGraph() {
        return graph;
    }

    public void showDirectedGraph(Map<String, Map<String, Integer>> G) {
        System.out.println("有向图结构:");
        for (Map.Entry<String, Map<String, Integer>> entry : G.entrySet()) {
            String source = entry.getKey();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                System.out.printf("%s -> %s [weight=%d]%n",
                        source, edge.getKey(), edge.getValue());
            }
        }
    }






    // 计算节点出度
    public int calculateOutDegree(String word) {
        word = word.toLowerCase();
        if (!graph.containsKey(word)) {
            return 0;
        }
        return graph.get(word).size();
    }

    // 计算节点入度
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

    // 计算两个节点之间的权重
    public int getEdgeWeight(String source, String target) {
        source = source.toLowerCase();
        target = target.toLowerCase();
        if (!graph.containsKey(source)) {
            return 0;
        }
        return graph.get(source).getOrDefault(target, 0);
    }


// 查找桥接词并按要求输出结果
    public void queryBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        // 检查word1和word2是否在图中
        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            System.out.println("No " + (!graph.containsKey(word1) ? word1 : word2) + " in the graph!");
            return;
        }

        List<String> bridges = findBridgeWords(word1, word2);

        if (bridges.isEmpty()) {
            System.out.println("No bridge words from " + word1 + " to " + word2 + "!");
        } else {
            String output = "The bridge words from " + word1 + " to " + word2 + " are: ";
            if (bridges.size() == 1) {
                output += bridges.get(0) + ".";
            } else {
                for (int i = 0; i < bridges.size(); i++) {
                    if (i == bridges.size() - 1) {
                        output += "and " + bridges.get(i) + ".";
                    } else {
                        output += bridges.get(i) + ", ";
                    }
                }
            }
            System.out.println(output);
        }
    }

    // 原始查找桥接词方法保持不变
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


    // 生成新文本插入桥接词
    public String generateNewText(String inputText) {
        // 处理输入文本：转换为小写，去除标点，分割单词
        String processed = inputText.replaceAll("[^a-zA-Z]", " ").toLowerCase();
        String[] inputWords = processed.split("\\s+");

        // 过滤空单词
        List<String> validWords = new ArrayList<>();
        for (String word : inputWords) {
            if (!word.isEmpty()) {
                validWords.add(word);
            }
        }

        if (validWords.size() < 2) {
            return inputText; // 不足两个单词无法插入桥接词
        }

        // 重建原始大小写的单词列表（用于输出）
        String[] originalWords = inputText.split("\\s+");
        List<String> originalValidWords = new ArrayList<>();
        for (String word : originalWords) {
            if (!word.replaceAll("[^a-zA-Z]", "").isEmpty()) {
                originalValidWords.add(word);
            }
        }

        // 构建新文本
        List<String> result = new ArrayList<>();
        result.add(originalValidWords.get(0)); // 添加第一个单词（保留原始大小写）

        Random random = new Random();
        for (int i = 0; i < validWords.size() - 1; i++) {
            String current = validWords.get(i);
            String next = validWords.get(i + 1);

            // 查找桥接词
            List<String> bridges = findBridgeWords(current, next);
            if (!bridges.isEmpty()) {
                // 随机选择一个桥接词（保持小写，或可以根据需要调整）
                String bridge = bridges.get(random.nextInt(bridges.size()));
                result.add(bridge);
            }

            // 添加下一个单词（保留原始大小写）
            result.add(originalValidWords.get(i + 1));
        }

        // 重建文本（保留原始标点和空格可能比较复杂，这里简化处理）
        return String.join(" ", result);
    }

    // 计算最短路径
    public List<String> calcShortestPath(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        if (!graph.containsKey(word1) || !graph.containsKey(word2)) {
            return Collections.emptyList();
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
            return Collections.emptyList(); // 没有路径
        }

        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        return path;
    }

    // 计算PageRank
    /**
     * @param d 阻尼系数，一般为0.85
     * @param maxIter 最大迭代次数
     * @param tol 收敛阈值
     * @return 节点到PageRank值的映射
     */
    public Map<String, Double> calculatePageRank(double d, int maxIter, double tol) {
        int N = graph.size();
        Map<String, Double> pr = new HashMap<>();
        // 初始化PR值
        for (String node : graph.keySet()) {
            pr.put(node, 1.0 / N);
        }
        // 迭代计算
        for (int iter = 0; iter < maxIter; iter++) {
            Map<String, Double> newPr = new HashMap<>();
            double diff = 0;
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
                double prValue = (1 - d) / N + d * sum;
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
     * 获取指定节点的PageRank值，使用默认阻尼系数(0.85)、迭代次数和收敛阈值
     * @param word 节点名称
     * @return PR值，如节点不存在返回0.0
     */
    public Double calPageRank(String word) {
        if (graph.isEmpty()) return 0.0;
        Map<String, Double> prMap = calculatePageRank(DEFAULT_D, DEFAULT_MAX_ITER, DEFAULT_TOL);
        return prMap.getOrDefault(word.toLowerCase(), 0.0);
    }

    // 辅助类用于优先队列
    private static class Node {
        String word;
        int distance;

        public Node(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }
    }

    // 随机游走
    public String randomWalk() throws IOException {
        if (graph.isEmpty()) return "";

        Random random = new Random();
        List<String> nodes = new ArrayList<>(graph.keySet());
        String current = nodes.get(random.nextInt(nodes.size()));

        BufferedWriter writer = new BufferedWriter(new FileWriter("walk.txt"));
        Scanner sc = new Scanner(System.in);

        // 记录起点
        System.out.println("起点节点: " + current);
        writer.write("节点: " + current);
        writer.newLine();
        StringBuilder sb = new StringBuilder();

        while (true) {
            Map<String,Integer> outEdges = graph.get(current);
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


    public static void main(String[] args) {
        TextGraphProcessor processor = new TextGraphProcessor();
        Scanner scanner = new Scanner(System.in);

        try {
            // 直接指定文件路径为input.txt
            String filePath = "D:\\JavaProject\\Software\\Lab1_v1\\src\\Easy Test.txt";
            System.out.println("正在读取文件: " + filePath);
            processor.processTextFile(filePath);

            System.out.println("\n文本处理完成，有向图已构建。");
            processor.showDirectedGraph(processor.getGraph());

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
                System.out.println("9. 退出");
                System.out.print("请输入选项: ");

                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("无效输入，请输入数字1-8。");
                    continue;
                }

                switch (choice) {
                    case 1:
                        System.out.print("请输入单词: ");
                        String word = scanner.nextLine();
                        System.out.printf("'%s'的出度为: %d%n", word, processor.calculateOutDegree(word));
                        break;
                    case 2:
                        System.out.print("请输入单词: ");
                        word = scanner.nextLine();
                        System.out.printf("'%s'的入度为: %d%n", word, processor.calculateInDegree(word));
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
                        processor.queryBridgeWords(word1, word2);
                        break;
                    case 5:
                        System.out.print("请输入新文本: ");
                        String newText = scanner.nextLine(); // 只需要一次nextLine()
                        System.out.println("生成的新文本: " + processor.generateNewText(newText));
                        break;
                    case 6:
                        System.out.print("请输入起始单词: ");
                        word1 = scanner.nextLine();
                        System.out.print("请输入目标单词: ");
                        word2 = scanner.nextLine();
                        List<String> path = processor.calcShortestPath(word1, word2);
                        if (path.isEmpty()) {
                            System.out.println("没有找到路径。");
                        } else {
                            System.out.println("最短路径: " + String.join(" -> ", path));
                            System.out.println("路径长度: " + (path.size()-1)); // 边数
                            // 计算总权重
                            int totalWeight = 0;
                            for (int i = 0; i < path.size()-1; i++) {
                                totalWeight += processor.getEdgeWeight(path.get(i), path.get(i+1));
                            }
                            System.out.println("总权重: " + totalWeight);
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
                        running = false;
                        break;
                    default:
                        System.out.println("无效选项，请输入1-8之间的数字。");
                }
            }
        } catch (IOException e) {
            System.err.println("处理文件时出错: " + e.getMessage());
            System.err.println("请确保input.txt文件存在于程序所在目录。");
        } finally {
            scanner.close();
            System.out.println("第二次修改");
            System.out.println("Change in IDE IJ");
        }
    }
}