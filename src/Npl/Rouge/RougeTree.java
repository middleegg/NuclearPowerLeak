package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.Reads;
import arc.util.io.Writes;

/**
 * RougeTree —— Rouge 模式的树杈状关卡结构（三分支）
 * ==============================================================
 * 规则：
 *   ① frozenForest 是根节点（起点）
 *   ② 通关根节点后分叉成三条线（左/中/右）
 *   ③ 玩家选一条线进入，另外两条线永久封锁
 *   ④ 每条线的最终地图是固定的（定义在 RougeMaps 中）
 *   ⑤ 每条线中间关卡从 rougeMaps 文件夹随机选取
 *
 * 存档：只需存「随机种子 + 已完成节点路径 + 选择的分支」，用种子重建同一棵树
 */
public class RougeTree {

    /** 节点类型枚举 */
    public enum NodeType {
        /** 进攻节点 - 有敌人核心需要摧毁 */
        ATTACK("进攻"),
        /** 防守节点 - 存活若干波次即可 */
        DEFENSE("防守"),
        /** 休息节点 - 无战斗，可以休整 */
        REST("休息"),
        /** 馈赠节点 - 免费获得资源 */
        GIFT("馈赠"),
        /** 商店节点 - 可以购买物品 */
        SHOP("商店"),
        /** 特殊节点 - 特殊事件或Boss */
        SPECIAL("特殊");

        public final String displayName;

        NodeType(String displayName) {
            this.displayName = displayName;
        }
    }

    /** 树节点 */
    public static class Node {
        public String mapName;       // 地图文件名（不含 .msav）
        public int id;               // 唯一 ID（用于存档）
        public Node parent;
        public Node left;            // 左子节点
        public Node middle;          // 中子节点
        public Node right;           // 右子节点
        public boolean locked = false;  // 被封锁
        public boolean completed = false; // 已占领
        public boolean collapsed = false; // 折叠状态
        public int depth = 0;        // 节点深度（用于分页）
        public NodeType type = NodeType.DEFENSE; // 节点类型（默认防守）
        // 布局坐标（用于绘制）
        public float x, y;

        public Node(){}
    }

    /** 根节点 */
    public static Node root;

    /** 随机种子 */
    public static long seed = 0;

    /** 所有可用地图（不含起点和终点） */
    private static Seq<String> availableMaps;

    /** 已使用的地图（避免重复） */
    private static ObjectSet<String> usedMaps = new ObjectSet<>();

    /** 节点计数器 */
    private static int nodeCounter = 0;

    /**
     * 生成树（用 seed 确保可重现）
     * @param seed 随机种子
     * @param maxDepth 最大深度（分支层数，包含终点）
     */
    public static void generateTree(long seed, int maxDepth){
        RougeTree.seed = seed;
        nodeCounter = 0;
        usedMaps.clear();
        availableMaps = RougeMaps.loadAvailableMaps();

        // 根节点 = frozenForest
        root = new Node();
        root.mapName = "frozenForest";
        root.id = nodeCounter++;
        usedMaps.add("frozenForest");

        // 用种子初始化随机数生成器
        Rand rand = new Rand(seed);

        // 从根节点开始，生成三条分支
        generateBranch(root, rand, maxDepth, 1, 0); // 左线
        generateBranch(root, rand, maxDepth, 1, 1); // 中线
        generateBranch(root, rand, maxDepth, 1, 2); // 右线

        // 计算布局坐标
        layoutTree(root);
    }

    /**
     * 递归生成一条分支
     * @param parent 父节点
     * @param rand 随机数生成器
     * @param maxDepth 最大深度
     * @param depth 当前深度
     * @param branchIndex 分支索引 (0=左, 1=中, 2=右)
     */
    private static void generateBranch(Node parent, Rand rand, int maxDepth, int depth, int branchIndex){
        if(depth > maxDepth) return;

        String mapName;

        // 如果是最后一层，使用固定的终点地图
        if(depth == maxDepth){
            mapName = RougeMaps.getEndMapName(branchIndex);
            if(mapName != null) availableMaps.remove(mapName);
        }else{
            // 随机选一个地图
            if(availableMaps.isEmpty()) return;
            mapName = availableMaps.random(rand);
            availableMaps.remove(mapName);
        }

        if(mapName == null) return;
        usedMaps.add(mapName);

        Node node = new Node();
        node.mapName = mapName;
        node.id = nodeCounter++;
        node.parent = parent;
        node.depth = depth;

        // 根据深度和随机数分配节点类型
        node.type = assignNodeType(rand, depth, maxDepth);

        // 根据分支索引设置子节点
        switch(branchIndex){
            case 0: parent.left = node; break;
            case 1: parent.middle = node; break;
            case 2: parent.right = node; break;
        }

        // 继续往下生成（不再分叉，每条线是线性的）
        generateBranch(node, rand, maxDepth, depth + 1, branchIndex);
    }

    /**
     * 根据随机数和深度分配节点类型
     * 规则：
     * - 根节点（depth=1）：防守
     * - 终点节点（depth=maxDepth）：特殊（Boss）
     * - 中间节点：随机分配（进攻、防守、休息、馈赠、商店）
     */
    private static NodeType assignNodeType(Rand rand, int depth, int maxDepth){
        // 根节点
        if(depth <= 1) return NodeType.DEFENSE;
        // 终点节点
        if(depth >= maxDepth) return NodeType.SPECIAL;

        // 中间节点随机分配
        int roll = rand.nextInt(100);
        if(roll < 30) return NodeType.ATTACK;      // 30% 进攻
        if(roll < 55) return NodeType.DEFENSE;     // 25% 防守
        if(roll < 70) return NodeType.REST;        // 15% 休息
        if(roll < 85) return NodeType.GIFT;        // 15% 馈赠
        return NodeType.SHOP;                       // 15% 商店
    }

    /**
     * 计算树节点的布局坐标
     * 左分支向左下延伸，中线垂直向下，右分支向右下延伸
     */
    private static void layoutTree(Node root){
        if(root == null) return;

        // 根节点居中
        root.x = 0;
        root.y = 0;

        // 左分支：向左偏移
        if(root.left != null){
            layoutBranch(root.left, -150f, -100f);
        }

        // 中线：垂直向下
        if(root.middle != null){
            layoutBranch(root.middle, 0f, -100f);
        }

        // 右分支：向右偏移
        if(root.right != null){
            layoutBranch(root.right, 150f, -100f);
        }
    }

    /** 线性分支的布局（每个节点向同方向偏移） */
    private static void layoutBranch(Node start, float offsetX, float offsetY){
        Node n = start;
        float x = start.parent.x + offsetX;
        float y = start.parent.y + offsetY;
        while(n != null){
            n.x = x;
            n.y = y;
            Node next = n.left != null ? n.left : (n.middle != null ? n.middle : n.right);
            if(next != null){
                next.x = x;
                next.y = y + offsetY;
            }
            n = next;
            y += offsetY;
        }
    }

    /** 获取节点的图标贴图 */
    public static TextureRegion getNodeIcon(String mapName){
        return RougeMaps.getNodeIcon(mapName);
    }

    /** 获取根节点 */
    public static Node getRoot(){
        return root;
    }

    /** 根据 ID 查找节点 */
    public static Node findById(int id){
        if(root == null) return null;
        Seq<Node> queue = new Seq<>();
        queue.add(root);
        while(!queue.isEmpty()){
            Node n = queue.first();
            queue.remove(0);
            if(n.id == id) return n;
            if(n.left != null) queue.add(n.left);
            if(n.middle != null) queue.add(n.middle);
            if(n.right != null) queue.add(n.right);
        }
        return null;
    }

    /**
     * 当玩家完成一个节点后：
     * - 如果是分叉点（根节点，有三条子线），玩家选一条，另外两条封锁
     * - 如果是线性节点，直接解锁下一个
     * @param nodeId 完成的节点 ID
     * @param chosenBranch 选择的分支 (0=左, 1=中, 2=右)，仅分叉点有效
     */
    public static void completeNode(int nodeId, int chosenBranch){
        Node node = findById(nodeId);
        if(node == null) return;
        node.completed = true;

        // 如果是分叉点（根节点有三条子线）
        if(node.left != null && node.middle != null && node.right != null){
            if(chosenBranch != 0) lockBranch(node.left);
            if(chosenBranch != 1) lockBranch(node.middle);
            if(chosenBranch != 2) lockBranch(node.right);
        }
    }

    /** 封锁整条分支（递归封锁所有子节点） */
    public static void lockBranch(Node start){
        if(start == null) return;
        start.locked = true;
        if(start.left != null) lockBranch(start.left);
        if(start.middle != null) lockBranch(start.middle);
        if(start.right != null) lockBranch(start.right);
    }

    /**
     * 获取从根到某个节点的路径（用于存档）
     */
    public static Seq<Integer> getPathTo(Node target){
        Seq<Integer> path = new Seq<>();
        Node n = target;
        while(n != null){
            path.add(n.id);
            n = n.parent;
        }
        path.reverse();
        return path;
    }

    /**
     * 获取已完成的节点 ID 集合（用于存档）
     */
    public static IntSeq getCompletedIds(){
        IntSeq ids = new IntSeq();
        if(root == null) return ids;
        Seq<Node> queue = new Seq<>();
        queue.add(root);
        while(!queue.isEmpty()){
            Node n = queue.first();
            queue.remove(0);
            if(n.completed) ids.add(n.id);
            if(n.left != null) queue.add(n.left);
            if(n.middle != null) queue.add(n.middle);
            if(n.right != null) queue.add(n.right);
        }
        return ids;
    }

    /** 获取当前选择路径的终点节点 */
    public static Node getCurrentEndNode(){
        if(root == null) return null;
        // 根据分支选择找到对应的终点
        if(root.left != null && !root.left.locked){
            return getLeafNode(root.left);
        }else if(root.middle != null && !root.middle.locked){
            return getLeafNode(root.middle);
        }else if(root.right != null && !root.right.locked){
            return getLeafNode(root.right);
        }
        return null;
    }

    /** 获取分支的叶子节点（最底端） */
    private static Node getLeafNode(Node node){
        if(node == null) return null;
        Node n = node;
        while(true){
            if(n.left != null) n = n.left;
            else if(n.middle != null) n = n.middle;
            else if(n.right != null) n = n.right;
            else break;
        }
        return n;
    }

    /** 检查树是否完全通关（当前路径终点已完成） */
    public static boolean isTreeComplete(){
        Node end = getCurrentEndNode();
        return end != null && end.completed;
    }

    /** 检查指定地图是否是终点地图 */
    public static boolean isEndMap(String mapName){
        return RougeMaps.isEndMap(mapName);
    }

    /** 获取指定分支的终点地图名称 */
    public static String getEndMapName(int branchIndex){
        return RougeMaps.getEndMapName(branchIndex);
    }

    /** 获取当前通关的终点地图名称 */
    public static String getCurrentEndMapName(){
        Node end = getCurrentEndNode();
        return end != null ? end.mapName : null;
    }

    // ==================== 联机同步（房主 → 客户端） ====================

    /**
     * 序列化整棵树（拓扑 + 锁定/完成/折叠状态）。
     *
     * 注意：绝不能只同步 seed 让客户端自己 generateTree —— generateTree 依赖
     * RougeMaps.loadAvailableMaps() 的文件系统遍历顺序（还有开发机的绝对路径），
     * 客户端机器上文件列表/路径不同就会生成出完全不同的树。
     * 直接把树写过去，客户端就完全不碰本机地图文件。
     */
    public static void writeTo(Writes w){
        w.l(seed);
        writeNode(w, root);
    }

    private static void writeNode(Writes w, Node n){
        w.i(n == null ? 0 : 1);
        if(n == null) return;

        w.i(n.id);
        w.str(n.mapName == null ? "" : n.mapName);
        w.i(n.type == null ? 0 : n.type.ordinal());
        w.i((n.locked ? 1 : 0) | (n.completed ? 2 : 0) | (n.collapsed ? 4 : 0));
        w.i(n.depth);

        writeNode(w, n.left);
        writeNode(w, n.middle);
        writeNode(w, n.right);
    }

    /** 用房主同步过来的数据重建树（客户端用；不读取本机地图文件） */
    public static void readFrom(Reads r){
        seed = r.l();
        root = readNode(r, null);

        // 恢复节点计数器，避免之后若有生成动作产生重复 id
        int max = -1;
        Seq<Node> queue = new Seq<>();
        if(root != null) queue.add(root);
        while(!queue.isEmpty()){
            Node n = queue.first();
            queue.remove(0);
            max = Math.max(max, n.id);
            if(n.left != null) queue.add(n.left);
            if(n.middle != null) queue.add(n.middle);
            if(n.right != null) queue.add(n.right);
        }
        nodeCounter = max + 1;

        // 客户端不需要地图池（留一个空 Seq，别留 null，避免之后误用）
        availableMaps = new Seq<>();
        usedMaps.clear();

        // 重算布局（TreePanel 建界面时还会再算一次，这里只是保证数据自洽）
        layoutTree(root);
    }

    private static Node readNode(Reads r, Node parent){
        if(r.i() == 0) return null;

        Node n = new Node();
        n.id = r.i();
        n.mapName = r.str();

        int type = r.i();
        NodeType[] types = NodeType.values();
        n.type = (type >= 0 && type < types.length) ? types[type] : NodeType.DEFENSE;

        int flags = r.i();
        n.locked = (flags & 1) != 0;
        n.completed = (flags & 2) != 0;
        n.collapsed = (flags & 4) != 0;
        n.depth = r.i();
        n.parent = parent;

        n.left = readNode(r, n);
        n.middle = readNode(r, n);
        n.right = readNode(r, n);
        return n;
    }
}
